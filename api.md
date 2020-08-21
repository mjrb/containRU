- /user
  - POST / {user, pass}
  - POST /login {user, pass}
    - returns jwt
  * /:id/priveledge?admin=true
    - make a user admin
  * GET /
    - lists users
- /instance
  - POST / {pass}
    - creates a DB container with the root password
  - GET /
    - lists that users dbs
    * ?all=true works for admins
  - GET /:id
    - info about an instance
  - DELETE /:id
    - removes an instance
* means admin feature thats not fully implemented
all of the instance routes require a header `Authorization: Token <your token>`