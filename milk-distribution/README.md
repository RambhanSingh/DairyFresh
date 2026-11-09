# Milk Distribution

A small personal web app to run a milk distribution business: customers, suppliers, inventory and accounts.
Built with Java 17, Spring Boot 3.5, Thymeleaf and an H2 file database.

It runs **only on your laptop** (`127.0.0.1`), has no public access and no login screen, because you are the only user.

## What you need

- JDK 17 or newer (`java -version`)
- Maven 3.9+ (`mvn -version`), or just open the folder in IntelliJ IDEA / Eclipse / VS Code and let it import `pom.xml`

## Run it

```
mvn spring-boot:run
```

or double-click `start.bat` (Windows) / run `./start.sh` (Mac, Linux).

Then open **http://localhost:8080** in your browser. Press `Ctrl+C` in the terminal to stop.
The first run downloads libraries from the internet, so it needs a connection once.

From an IDE: run the `main` method of `com.milk.MilkApplication`.

## Optional: build one runnable file

```
mvn package
java -jar target/milk-distribution.jar
```

## The four sections

| Section | What it does |
|---|---|
| **Customers** | Add customers with phone, location, daily quantity and rate. Each customer has a ledger (deliveries, payments, running balance). **Daily deliveries** records a day's milk for all active customers in one click, and you can change a quantity when a customer took more or less. |
| **Suppliers** | Add suppliers, record milk bought and payments made. Each supplier has a ledger and a "still to pay" balance. |
| **Inventory** | Litres bought, sold, wasted and in stock, average cost per litre, value of stock, days of supply left, day-by-day movement. Add purchases and wastage here. |
| **Accounts** | Total invested, paid to suppliers, pending to pay suppliers, total billed, received from customers, pending collection, net cash, estimated profit, and who owes what. |

## Your data

Everything is stored in the **`data`** folder next to the app (`data/milkdb.mv.db`).

- **Backup:** stop the app, then copy the `data` folder somewhere safe (a pen drive or Google Drive).
- **Restore:** stop the app and put the `data` folder back.
- To start again from an empty database, stop the app and delete the `data` folder.

## Privacy

`server.address=127.0.0.1` in `src/main/resources/application.properties` means the app answers only to this laptop.
Nobody on your Wi-Fi or the internet can open it. Do not change that to `0.0.0.0` unless you add a login first.

## How the numbers work

- **Billed** = litres delivered x rate on that day (the rate is saved with each delivery, so changing a customer's rate later does not change old bills).
- **Pending collection** = billed - received, per customer.
- **Pending to pay** = milk bought - paid, per supplier.
- **Stock** = litres bought - litres delivered - litres wasted.
- **Estimated profit** = total billed - (litres sold + wasted) x average purchase cost per litre.
- **Net cash** = received from customers - paid to suppliers.

## Changing the port

Edit `server.port` in `src/main/resources/application.properties`.
