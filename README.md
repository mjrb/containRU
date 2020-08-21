# ContainRU

A "cloud" service for running containers for users. for example you can host mysql db instances.
It also implements a tcp proxy service that automatically sleeps containers when not in use and
wakes them when something tries to connect to its port

## Usage

- `systemctl start mongod`
- `lein uberjar` or use one that may be in the repo already
- `java -jar <path to uberjar>`
- TODO run something to host the frontend

## API
[see here](api.md)

# TODO
- create a frontend
- support multiple container types
- allow users to upload custom containers
- set a user's container quota
- set container cpu, mem and disk quotas
- admin functionality for controlling users from the web
- add the ability for users to upload/download files from their volumes
- add more volume drivers to potentially scale storage
- add a container driver for scaling compute across multiple nodes using kubernetes or docker swarm
- add the ability to proxy https on port 443 for users running web apps
- [really a lot of work and not necissary] allow scalling of tcp proxy accross multiple ingress nodes
  - there are about 55535 potentially useable ports and i don't think we would or should ever get that high

## License
    Copyright (C) 2020 Mickey J Winters

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
