const API_URL = "distributori";

$(document).ready(function() {
    initMap();
    caricaDistributori();

    $("#refreshBtn").click(function() {
        caricaDistributori();
    });

    setInterval(caricaDistributori, 5000);

    // Gestione Form Aggiunta
    $("#addForm").submit(function(event) {
        event.preventDefault();

        let idManuale = $("#idDistributore").val();
        let latVal = parseFloat($("#latitudine").val());
        let lonVal = parseFloat($("#longitudine").val());

        let dati = {
            id: idManuale,
            latitudine: latVal,
            longitudine: lonVal,
            stato: "ATTIVO"
        };

        $.ajax({
            url: API_URL,
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(dati),
            success: function() {
                alert("Distributore salvato!");
                $("#addForm")[0].reset();
                caricaDistributori();
            },
            error: function(xhr) {
                alert("Errore nel salvataggio. Forse l'ID esiste già?");
                console.error(xhr);
            }
        });
    });
});

let map;
let markers = [];

function initMap() {
    map = L.map('map').setView([45.4642, 9.1900], 10);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);
}

function caricaDistributori() {
    $.getJSON(API_URL, function(data) {
        popolaTabella(data);
        aggiornaMappa(data);
    }).fail(function() {
        console.error("Errore comunicazione con la Servlet.");
    });
}

function popolaTabella(data) {
    let tbody = $("#distributoriTableBody");
    tbody.empty();

    if (data.length === 0) {
        // Nota: colspan=6 perché abbiamo rimesso la colonna Azioni
        tbody.append("<tr><td colspan='6' class='text-center'>Nessun distributore presente</td></tr>");
        return;
    }

    data.forEach(d => {
        let ultimoHB = d.ultimoHeartbeat ? new Date(d.ultimoHeartbeat).toLocaleString() : "Mai";

        let classeStato = 'text-secondary';
        if (d.stato === 'ATTIVO') classeStato = 'text-success fw-bold';
        if (d.stato === 'GUASTO') classeStato = 'text-danger fw-bold';
        if (d.stato === 'MANUTENZIONE') classeStato = 'text-warning fw-bold';

        // QUI SONO RIMASTI SOLO I 3 PULSANTI CHE VOLEVI
        let row = `
            <tr>
                <td>${d.id}</td>
                <td>${d.latitudine}</td>
                <td>${d.longitudine}</td>
                <td class="${classeStato}">${d.stato}</td>
                <td>${ultimoHB}</td>
                <td>
                    <button class="btn btn-primary btn-sm" onclick="heartbeat('${d.id}')">Heartbeat</button>
                    <button class="btn btn-success btn-sm" onclick="attiva('${d.id}')">Attiva</button>
                    <button class="btn btn-warning btn-sm" onclick="manutenzione('${d.id}')">Manutenzione</button>
                </td>
            </tr>
        `;
        tbody.append(row);
    });
}

function aggiornaMappa(data) {
    markers.forEach(m => map.removeLayer(m));
    markers = [];

    data.forEach(d => {
        if (d.latitudine != null && d.longitudine != null) {
            let colorePallino = 'blue';
            if (d.stato === 'ATTIVO') colorePallino = 'green';
            else if (d.stato === 'GUASTO') colorePallino = 'red';
            else if (d.stato === 'MANUTENZIONE') colorePallino = 'orange';

            let marker = L.circleMarker([d.latitudine, d.longitudine], {
                color: colorePallino,
                fillColor: colorePallino,
                fillOpacity: 0.8,
                radius: 10
            }).bindPopup(`
                <b>ID:</b> ${d.id}<br>
                <b>Stato:</b> <span style="color:${colorePallino}; font-weight:bold;">${d.stato}</span>
            `);

            marker.addTo(map);
            markers.push(marker);
        }
    });
}

// --- FUNZIONI PULSANTI (Ho tolto 'elimina') ---

function heartbeat(id) {
    $.ajax({
        url: API_URL + "/" + id + "/heartbeat",
        type: "POST",
        success: function() { caricaDistributori(); }
    });
}

function attiva(id) {
    $.ajax({
        url: API_URL + "/" + id + "?stato=ATTIVO",
        type: "PUT",
        success: function() { caricaDistributori(); }
    });
}

function manutenzione(id) {
    $.ajax({
        url: API_URL + "/" + id + "?stato=MANUTENZIONE",
        type: "PUT",
        success: function() { caricaDistributori(); }
    });
}