#!/bin/bash
# =============================================================
# Wrapper entrypoint cho SQL Server Docker container.
# 1. Khởi động SQL Server ở background
# 2. Chờ SQL Server sẵn sàng
# 3. Chạy init.sql để tạo DB + user
# 4. Giữ SQL Server chạy ở foreground
# =============================================================
set -e

echo ">>> Starting SQL Server..."
/opt/mssql/bin/sqlservr &
MSSQL_PID=$!

echo ">>> Waiting for SQL Server to be ready..."
for i in $(seq 1 60); do
    /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -Q "SELECT 1" &>/dev/null && break
    echo "    ...attempt $i/60"
    sleep 1
done

# Chạy init script (ĐÃ VÁ LỖI CODE SMELL BẰNG NGOẶC KÉP)
if [[ -f "/docker-entrypoint-initdb.d/init.sql" ]]; then
    echo ">>> Running init.sql..."
    /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -i /docker-entrypoint-initdb.d/init.sql
    echo ">>> Init complete!"
else
    echo ">>> No init.sql found, skipping."
fi

# Giữ container chạy
wait $MSSQL_PID