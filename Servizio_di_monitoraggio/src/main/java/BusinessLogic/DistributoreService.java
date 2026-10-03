package BusinessLogic;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions; // Importante per creare se non esiste
import com.mongodb.client.model.Updates;
import Model_Entity.Distributore;
import Model_Entity.StatoDistributore;
import jakarta.annotation.PostConstruct; // Importante
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.bson.conversions.Bson;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped //La uso per dire all'app di creare una sola instanza
public class DistributoreService {

    @Inject
    private MongoDatabase db;

    private LocalDateTime ultimaSincronizzazione = null;
    private static final int MINUTI_COOLDOWN = 1; // Riprova a sincronizzare al massimo ogni minuto

    private static final Duration TIMEOUT_GUASTO = Duration.ofSeconds(180); // 3 minuti come da richiesta (era 6000)

    //eseguo il metodo dopo aver creato l oggetto con inject
    @PostConstruct
    public void avvioAutomatico() {
        new Thread(() -> {
            System.out.println("Sincronizzazione XML con Spring...");
            sincronizzaStatiEsterni();
            System.out.println("Procedura di avvio terminata");
        }).start();
    }

    private MongoCollection<Distributore> getCollection() {
        return db.getCollection("distributori", Distributore.class);
    }

    public void salvaDistributore(Distributore d) {
        if (d.getId() == null || d.getId().isEmpty()) {
            d.setId(UUID.randomUUID().toString());
        }
        if (d.getStato() == null) {
            d.setStato(StatoDistributore.MANUTENZIONE);
        }

        getCollection().replaceOne(
                Filters.eq("_id", d.getId()),
                d,
                new ReplaceOptions().upsert(true)
        );
        System.out.println("Salvataggio/Aggiornamento Mongo completato per ID: " + d.getId());
    }



    //MESSO IN CASO DI IMPLEMENTAZIONE DI ELOIMINAZIONE DEI DISTRIBUTORI DAL DATABASE DEL APP DI MONITORAGGIO
    //SUCCESSIVAMENTE DA IMPLEMENTARE CON UNA SERVICE CHE FA LA CHIAMATA ALL'APP SPRING PER ELIMINARE IL DISTRIBUTORE ANCHE LI
    public void cancellaDistributore(String id) {
        getCollection().deleteOne(Filters.eq("_id", id));
        System.out.println("Eliminazione da Mongo completata per ID: " + id);
    }



    public void riceviHeartbeat(String id) {
        Distributore d = getCollection().find(Filters.eq("_id", id)).first();

        if (d == null) {
            throw new WebApplicationException("Distributore non trovato: " + id, Response.Status.NOT_FOUND);
        }

        List<Bson> aggiornamenti = new ArrayList<>();
        aggiornamenti.add(Updates.set("ultimoHeartbeat", LocalDateTime.now()));

        if (d.getStato() != StatoDistributore.MANUTENZIONE) {
            aggiornamenti.add(Updates.set("stato", StatoDistributore.ATTIVO));
        }

        if (!aggiornamenti.isEmpty()) {
            getCollection().updateOne(Filters.eq("_id", id), Updates.combine(aggiornamenti));
        }
    }

    public List<Distributore> getTuttiDistributori() {
        aggiornaStatiGuasti();

        if (ultimaSincronizzazione == null ||
                ChronoUnit.MINUTES.between(ultimaSincronizzazione, LocalDateTime.now()) >= MINUTI_COOLDOWN) {

            CompletableFuture.runAsync(() -> {
                System.out.println("Avvio sincronizzazione background");
                sincronizzaStatiEsterni();
            });
        }

        return getCollection().find().into(new ArrayList<>());
    }

    public void aggiornaStatoManuale(String id, StatoDistributore nuovoStato) {
        getCollection().updateOne(Filters.eq("_id", id), Updates.set("stato", nuovoStato));
    }

    public void sincronizzaStatiEsterni() {
        try {

            ultimaSincronizzazione = LocalDateTime.now();
            URL url = new URL("http://localhost:8080/api/export/xml");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);

            if (conn.getResponseCode() != 200) {
                System.out.println("Spring Boot non risponde (Codice " + conn.getResponseCode() + ")");
                return;
            }

            InputStream is = conn.getInputStream();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(is);

            doc.getDocumentElement().normalize();
            NodeList nodi = doc.getElementsByTagName("Distributore");

            System.out.println("Trovati " + nodi.getLength() + " distributori nell'XML.");

            for (int i = 0; i < nodi.getLength(); i++) {
                Element elemento = (Element) nodi.item(i);

                String id = getTagValue("Id", elemento);
                String statoXML = getTagValue("Stato", elemento);
                String latStr = getTagValue("Latitudine", elemento);
                String lonStr = getTagValue("Longitudine", elemento);

                if (id != null) {
                    Distributore d = new Distributore();
                    d.setId(id);

                    if (latStr != null && !latStr.isEmpty()) d.setLatitudine(Double.parseDouble(latStr));
                    if (lonStr != null && !lonStr.isEmpty()) d.setLongitudine(Double.parseDouble(lonStr));

                    StatoDistributore nuovoStato = StatoDistributore.ATTIVO;
                    if ("Manutenzione".equalsIgnoreCase(statoXML)) {
                        nuovoStato = StatoDistributore.MANUTENZIONE;
                    } else if ("Guasto".equalsIgnoreCase(statoXML)) {
                        nuovoStato = StatoDistributore.GUASTO;
                    }
                    d.setStato(nuovoStato);

                    Distributore esistente = getCollection().find(Filters.eq("_id", id)).first();
                    if (esistente != null) {
                        d.setUltimoHeartbeat(esistente.getUltimoHeartbeat());
                    }

                    getCollection().replaceOne(
                            Filters.eq("_id", id),
                            d,
                            new ReplaceOptions().upsert(true)
                    );
                }
            }
            System.out.println("Database allineato con Spring.");

        } catch (Exception e) {
            System.err.println(" Impossibile sincronizzare XML App principale spenta " + e.getMessage());
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private void aggiornaStatiGuasti() {
        List<Distributore> daControllare = getCollection()
                .find(Filters.not(Filters.eq("stato", StatoDistributore.MANUTENZIONE.name())))
                .into(new ArrayList<>());

        LocalDateTime now = LocalDateTime.now();

        for (Distributore d : daControllare) {
            if (d.getUltimoHeartbeat() == null) {
                if (d.getStato() != StatoDistributore.GUASTO) {
                    impostaGuasto(d.getId());
                    System.out.println("Distributore " + d.getId() + " marcato GUASTO ");//guasto perche non l ho mai ricevuto quindi lo considero guasto
                }
                continue;
            }
            Duration tempoTrascorso = Duration.between(d.getUltimoHeartbeat(), now);

            if (tempoTrascorso.compareTo(TIMEOUT_GUASTO) > 0 && d.getStato() != StatoDistributore.GUASTO) {
                impostaGuasto(d.getId());
                System.out.println("Distributore " + d.getId() + " marcato GUASTO (Timeout Heartbeat)");
            }
        }
    }

    private void impostaGuasto(String id) {
        getCollection().updateOne(Filters.eq("_id", id), Updates.set("stato", StatoDistributore.GUASTO));
    }
}