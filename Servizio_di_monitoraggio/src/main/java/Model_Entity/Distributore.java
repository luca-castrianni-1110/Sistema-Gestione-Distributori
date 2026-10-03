package Model_Entity;

import jakarta.json.bind.annotation.JsonbProperty;
import org.bson.codecs.pojo.annotations.BsonId;
import java.time.LocalDateTime;

public class Distributore {


    //chiave primaria per mongoDB
    @BsonId
    private String id;

    //uso per rinnominare i cmapi quando trasformo gli oggetti in json
    @JsonbProperty("latitudine")
    private Double latitudine;
    @JsonbProperty("longitudine")
    private Double longitudine;
    private StatoDistributore stato;
    private LocalDateTime ultimoHeartbeat;

    public Distributore() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Double getLatitudine() { return latitudine; }
    public void setLatitudine(double latitudine) { this.latitudine = latitudine; }

    public Double getLongitudine() { return longitudine; }
    public void setLongitudine(double longitudine) { this.longitudine = longitudine; }

    public StatoDistributore getStato() { return stato; }
    public void setStato(StatoDistributore stato) { this.stato = stato; }

    public LocalDateTime getUltimoHeartbeat() { return ultimoHeartbeat; }
    public void setUltimoHeartbeat(LocalDateTime ultimoHeartbeat) { this.ultimoHeartbeat = ultimoHeartbeat; }

    @Override
    public String toString() {
        return "Distributore{id='" + id + "', stato=" + stato + "}";
    }
}