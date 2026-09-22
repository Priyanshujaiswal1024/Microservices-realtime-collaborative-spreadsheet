#!/bin/bash
set -e

# This script creates separate databases for each microservice
# Run during PostgreSQL container initialization

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- User Service Database
    CREATE DATABASE userdb;
    GRANT ALL PRIVILEGES ON DATABASE userdb TO postgres;

    -- Sheet Service Database
    CREATE DATABASE sheetdb;
    GRANT ALL PRIVILEGES ON DATABASE sheetdb TO postgres;

    -- Comment Service Database
    CREATE DATABASE commentdb;
    GRANT ALL PRIVILEGES ON DATABASE commentdb TO postgres;

    -- Notification Service Database
    CREATE DATABASE notificationdb;
    GRANT ALL PRIVILEGES ON DATABASE notificationdb TO postgres;

    -- Audit Service Database
    CREATE DATABASE auditdb;
    GRANT ALL PRIVILEGES ON DATABASE auditdb TO postgres;

    -- List all databases
    \l
EOSQL

echo "All databases created successfully!"
