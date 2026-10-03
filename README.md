# Sistema-Gestione-Distributori
Piattaforma web Full-Stack e distribuita per la gestione, telemetria e monitoraggio multi-ruolo di distributori automatici (Java, Jakarta EE, MongoDB, Thymeleaf)


# Piattaforma Web per la Gestione e Monitoraggio di Distributori Automatici (Vending Machine System)

Piattaforma web Full-Stack e distribuita, progettata per la simulazione, il controllo operativo, la telemetria e la manutenzione di una rete di distributori automatici intelligenti. Il sistema gestisce l'intera logica di interazione attraverso un'architettura multi-ruolo protetta (**RBAC - Role-Based Access Control**).

---

## Obiettivo del Progetto
Fornire un ecosistema centralizzato in grado di:
1. **Gestire le transazioni degli utenti finali** (gestione del credito, ricariche con carta, selezione e personalizzazione di bevande calde).
2. **Monitorare in tempo reale lo stato di salute e le scorte delle macchine** tramite telemetria, controllo di heartbeat asincrono e sincronizzazione dei dati via XML.
3. **Fornire agli amministratori e ai tecnici** strumenti dedicati per la diagnostica hardware, il ripristino dei guasti, il controllo delle scorte di magazzino e la gestione globale della flotta di distributori e del personale.

---

## Tecnologie e Componenti Principali
* **Backend & Business Logic:** Java, Jakarta EE, Servlet API, pattern architetturali enterprise (`@ApplicationScoped`, `@PostConstruct`).
* **Concorrenza & Asincronia:** Programmazione asincrona con `CompletableFuture` e thread in background per la sincronizzazione continua dei dati.
* **Database & Persistenza NoSQL:** MongoDB Atlas configurato tramite `MongoConfig` con l'ausilio di BSON POJO Codec per la mappatura automatica delle entità.
* **Integrazione & Scambio Dati:** Parsing di flussi XML ed esportazione dati.
* **Frontend & Interattività:** HTML5, CSS3, Thymeleaf, JavaScript, jQuery, AJAX per aggiornamenti dinamici delle dashboard live.

---

## Architettura dei Ruoli e Moduli

### 1. Area Cliente / Utente (`Schermata_Utente.html`, `Schermata_Distributore.html`)
* **Profilo & Credito:** Autenticazione sicura, visualizzazione del saldo e ricarica del credito (tramite tagli fissi o importi personalizzati con simulazione carta di credito).
* **Simulazione Distributore:** Connessione alla macchina tramite ID dedicato.
* **Selezione Bevande:** Scelta tra Espresso, Caffè Lungo, Macchiato, Cappuccino, Ginseng, Orzo, Cioccolata e Thè al limone, con personalizzazione granulare del livello di zucchero (No, Poco, Medio, Molto).
* **Erogazione:** Schermata animata di preparazione e ritiro del prodotto (`Schermata_Caricamento_Acquisto.html`).

### 2. Area Addetto alla Manutenzione (`Schermata_Addetto_Manutenzione.html`)
* **Dashboard Operativa:** Visualizzazione rapida dei distributori assegnati.
* **Diagnostica Hardware:** Monitoraggio in tempo reale dello stato di componenti critici (pompa acqua, erogatore liquido) e funzioni di ripristino o riparazione rapida.
* **Controllo Scorte:** Monitoraggio dei livelli di ingredienti (caffè, decaf, orzo, ginseng, thè, latte, cioccolata, zucchero, acqua) e materiali di consumo (bicchieri, palette) con opzione di rifornimento globale.

### 3. Area Gestore del Sistema (`Schermata_Gestore_Del_Sistema.html`)
* **Panoramica Globale:** Ricerca live e monitoraggio centralizzato di tutti i distributori e dei manutentori attivi nel sistema.
* **Gestione Macchine:** Inserimento geolocalizzato di nuovi distributori (con coordinate di latitudine/longitudine e credenziali di accesso dedicate) o rimozione dal sistema.
* **Gestione del Personale:** Creazione di nuovi account per i manutentori (con validazione rigorosa della password e dei requisiti di sicurezza) o revoca degli accessi.

---

## Telemetria e Monitoraggio Avanzato (`DistributoreService.java`)
* **Heartbeat Asincrono:** Le macchine inviano regolarmente segnali di vita (`/heartbeat`). Se non vengono ricevuti segnali entro una soglia di timeout impostata (3 minuti), il sistema marchia automaticamente il distributore come `GUASTO`.
* **Sincronizzazione Esterna:** Connessione periodica automatica verso endpoint remoti in formato XML per allineare lo stato dei distributori con il database centrale MongoDB.

---

## Guida all'Utilizzo (Metodo di Caricamento `.zip`)

Poiché il progetto è composto da un numero elevato di file strutturati in package, l'intero codice sorgente è stato compresso nell'archivio `.zip` presente in questa repository.

1. **Download:** Scarica l'archivio compresso del progetto sul tuo computer.
2. **Estrazione:** Scompatta la cartella in una directory di lavoro locale.
3. **Configurazione:** 
   * Configura la stringa di connessione a **MongoDB Atlas** all'interno della classe `MongoConfig.java`.
   * Assicurati che l'ambiente di servlet/server Java (es. WildFly, TomEE o container compatibile) sia configurato correttamente.
4. **Avvio:** Compila ed esegui l'applicazione; il metodo `@PostConstruct` avvierà automaticamente i thread di sincronizzazione e telemetria in background.
