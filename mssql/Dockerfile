FROM mcr.microsoft.com/mssql/server:2019-latest
COPY ./wait-for-sqlserver.sh /wait-for-sqlserver.sh
RUN chmod +x /*.sh
CMD /opt/mssql/bin/sqlservr
