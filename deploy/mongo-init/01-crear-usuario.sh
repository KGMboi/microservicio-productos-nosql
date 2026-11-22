#!/bin/bash
# Se ejecuta una sola vez, cuando el volumen de MongoDB está vacío.
# Crea un usuario de la aplicación con permisos solo sobre la base productosdb.
mongosh --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin <<JS
use productosdb
db.createUser({
  user: "$APP_DB_USER",
  pwd: "$APP_DB_PASSWORD",
  roles: [{ role: "readWrite", db: "productosdb" }]
})
JS
