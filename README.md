# MS-PAYMENT

## Micro service payment

### Create base and docker
    
```bash
sh ./script/create_db.sh
```

### Remove base and docker

```bash
docker compose down
```

The script will run docker compose and create the database with the sql file.

### Utilisation
If you want to create a new entity (new table) create the table in the sql file and create entity from this table with
JPA buddy.

### Description

This microservice is responsible for the payment of the users. It will receive the user's credentials and will return a token that will be used to authenticate the user in the other micro services.

### Endpoints

Swagger available at : http://localhost:8080/api/ms-payment/swagger-ui/index.html