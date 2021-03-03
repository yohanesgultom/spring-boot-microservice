FROM mcr.microsoft.com/mssql/server:2019-latest
USER root
COPY ./wait-for-sqlserver.sh /wait-for-sqlserver.sh
RUN chmod 755 /*.sh
CMD /opt/mssql/bin/sqlservr
