# Json container

This project is about storing jsons in h2 database. Main goal is to build
compact store for json objects. If one has many json objects that are very
similar in structure, fields and data, this way of storing them may introduce
storage savings.

## Database

Json container project uses `h2` database by default. `sqlite` support is
planned yet not started.
