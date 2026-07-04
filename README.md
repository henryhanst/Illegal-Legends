# ITI0301-2024
## Illegal Legends

### Mängu kirjeldus

MOBA stiilis mäng, aga ainult ühe rajaga (nagu League of Legends Aram). Mäng on 2.5D, 3vs3 ja toimub pärismaailmast inspireeritud keskkonnas.
Lahing käib linnatänaval, kus baasid on paigutatud üksteisega vastamisi.
Kõik mängijad tekivad oma baasi juures nii mängu alguses kui ka taas ellu ärgates.
Samuti tekivad baasi juures „minionid“, kes jooksevad vastase baasi poole.
Kui minionid surevad saavad mängijad XP-d. Mängijal on võimalik koguda XP-d, et tõusta uuele tasemele, mille tulemusena suureneb tema tegelase võimekus.
Mängus on erinevad karakterid, kellel on erioskused ehk võimed.

Eesmärk on hävitada vastase baas.



---

## Nupud mängimiseks

| Tegevus                 | Klahv      |
|-------------------------|------------|
| Liikumine / Auto-attack | Paremklõps |
| Võime 1                 | Q          |
| Võime 2                 | W          |
| Kinnine/lahtine kaamera | Y          |
| Lahku mängust           | Esc        |

---


## Karakterid ja võimed

| Karakter | Võime Q          | Võime W |
|----------|------------------|---------|
| Ranged   | Skillshot        | Flash   |
| Fighter  | Empowered attack | Stun    |
| Tank     | Rage             | Heal    |


---

### Juhend mängimiseks

1. Kui server ja client juba käivad, siis tuleb vajutada start.
2. Peale starti saab valida omale lobby, kus mängida.
3. Peale lobbyga liitumist tuleb tiim valida ja soovi korral saab muuta oma nime.
4. Kui kõik on lobbys tuleb vajutada "Start Game".
5. Siis saab valida oma karakteri ja vajutada "Lock in" ja kui kõik on vajutanud "Lock in" algab mäng.
6. Mängus saab liikuda ja rünnata kasutades paremat klõpsu.
7. Võimeid saab kasutada Q ja W klahvidega.
8. Kui vastasele pihta lasta, siis vastane saab haiget.
9. Paremat klõpsu vastase peale vajutades, hakkab mängija karakter teda jälitama ja piisavalt lähedale jõudes lööb vastast.
10. Eesmärgiks on hävitada alguses vastase turret ja siis baas.

## Mängu käivitamine lokaalselt
1. Klooni see repositoorium: https://gitlab.cs.taltech.ee/oslapi/iti0301-2026

#### Intellij IDEAs:
#### - Server:
2. Kontrolli, et serveri kaust oleks moodul
3. Liigu `iti0301-2026-server -> server -> src -> main -> java -> ee.taltech.examplegame.server -> ServerLauncher`
4. Pane see file käima.

#### - Client:
2. Liigu `lwjgl3 -> src -> main -> java -> ee.taltech.examplegame.lwjgl3 -> Lwjgl3Launcher`
3. Pane see fail käima.

---
#### Terminalis käivitamine:
Repositooriumi root directorys järgmised käsud:
#### Server:

`./gradlew :server:run`

#### Client:
`./gradlew :lwjgl3:run`


---

## Tehnoloogiad

| Tehnoloogia      | Versioon | Kasutus                        |
|------------------|----------|--------------------------------|
| Java             | 21       | Programmeerimiskeel            |
| LibGDX           | 1.13.1   | Mänguarenduse raamistik        |
| Kryonet          | 2.22     | Võrgusuhtlus                   |
| Gradle           | 8.10.2   | Ehitusautomaatika              |
| Lombok           | 1.18.36  | Koodi lühendamise teek         |
 
---

### Omadused:
| Omadus                | Seletus                                                                                                                       |
|-----------------------|-------------------------------------------------------------------------------------------------------------------------------|
| Multiplayer support   | 3 vs 3 ehk mängus kuni 6 korraga                                                                                              |
| Lobby süsteem         | Valik lobbysid, kuhu mängijad liituvad                                                                                        |
| Combat                | Vastast saab rünnata ability või auto-attackidega                                                                             |
| Karakteri valimine    | Enne mängimise alustamist on võimalik karakterit valida                                                                       |
| NPC Minionid          | Minionid, kes kaklevad üksteisega, löövad mängijaid ja objekte                                                                |
| Lõhutavad struktuurid | Struktuurid, mida mängija ja minionid löövad, et mängu võita. Enne ei saa järgmise struktuuri juurde minna, kui eelmine on lõhutud |
| Settings menüü        | Saab valida erinevate resolutsioonide vahel ja muuta heli taset                                                               |
| Helid                 | Mängus on muusika champ-selectis, menüüs, mängu ajal, nuppude vajutamisel ja on ka announcer.                                 |
| XP                    | Kui minionid surevad, saab karakter XP-d ja läbi selle suureneb level ja karakteri tugevus.                                   |
| Kaasahaarav UX        | Ilus GUI, abilititel on heli efektid ja mängul on taustamuusika                                                               |
| Keeruline pathfinding | Pathfinding kasutades A*, BFS ja Bresenhami algoritmi                                                                         |



### Autorid:
- Osmo Lapin
- Henry Hanst
- Kristjan Kahro
