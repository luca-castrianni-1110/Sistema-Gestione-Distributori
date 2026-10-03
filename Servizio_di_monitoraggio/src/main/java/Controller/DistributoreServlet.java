package Controller;

import BusinessLogic.DistributoreService;
import Model_Entity.Distributore;
import Model_Entity.StatoDistributore;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import jakarta.xml.bind.annotation.*;

@WebServlet(name = "DistributoreServlet", urlPatterns = { "/distributori/*" }) // lo uso per trasformare la classe java
                                                                               // in una porta web
public class DistributoreServlet extends HttpServlet {

    @Inject // non scrivo service = new DistributoreService() ma solo:
    private DistributoreService service;

    private final Jsonb jsonb = JsonbBuilder.create();

    // Lista dei distributori
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setHeader("Access-Control-Allow-Origin", "*");// mi serve per chiedere i dati da una porta diversa senxa
                                                           // che si blocchi tutto

        List<Distributore> lista = service.getTuttiDistributori();
        resp.getWriter().write(jsonb.toJson(lista));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");

        String path = req.getPathInfo();

        // se nel URL finale c'è scritto heartbeat e aggiorna la data
        if (path != null && path.endsWith("/heartbeat")) {
            String[] parts = path.split("/");
            if (parts.length >= 2) {
                String idDistributore = parts[1];
                service.riceviHeartbeat(idDistributore);
                System.out.println("Heartbeat ricevuto per ID: " + idDistributore);
                resp.setStatus(200);
            } else {
                resp.setStatus(400);
            }
            return;
        }

        // Aggiungo il distributore

        // Se non c'è legge il corpo dell url e il json lo trasforma in un oggetto e lo
        // salva
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null)
            buffer.append(line);

        try {
            Distributore d = jsonb.fromJson(buffer.toString(), Distributore.class);

            service.salvaDistributore(d);

            resp.setStatus(201);
            System.out.println("Nuovo distributore aggiunto: " + d.getId());

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // aggiorno lo stato io
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        String statoParam = req.getParameter("stato");

        if (path != null && path.length() > 1 && statoParam != null) {
            try {
                String id = path.substring(1);

                StatoDistributore nuovoStato = StatoDistributore.valueOf(statoParam);

                service.aggiornaStatoManuale(id, nuovoStato);

                resp.setStatus(200);
                System.out.println("Stato aggiornato per ID " + id + ": " + nuovoStato);

            } catch (IllegalArgumentException e) {
                resp.setStatus(400);
                System.err.println("Stato non valido: " + statoParam);
            } catch (Exception e) {
                e.printStackTrace();
                resp.setStatus(500);
            }
        } else {
            resp.setStatus(400);
        }
    }

}