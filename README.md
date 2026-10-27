# Project Details

## Project Description
Fish-project is a WIP web-application for documenting fishing trips.
The interface is available in english and german, but currently only german fish
and fish-names are stored in the database.

## Currently implemented features
- User account creation with cloudflare turnstile captcha and JWT token based login
- User password change and deletion
- Creation of fishing trips with info
- Creation of simple counted fish-catches or detailed catches with images and
  additional data
- Editing of trips and their catches
- Infinite scrolling trip view with expanding information and search filters
- Infinite scrolling gallery view of special catches with search filters

# Running
The `Makefile` in root has targets for running, testing and cleaning up, `make` lists all of them.

## Docker quickstart
To quickly start the project create .env file in root with the following content, change all placeholder values if desired
(especially `JWT_SECRET` should be a random valid secret jwt key) and run `make up`.
Afterwards connect to the frontend on a browser (`http://localhost:3030/` in this example)
```
  DB_HOST=localhost
  DB_PORT=3306
  DB_DBNAME=springdb
  DB_USERNAME=maria
  DB_PASSWORD=mariapass
  DB_ROOT_PASSWORD=rootpass
  BACKEND_PORT=8080
  FRONTEND_PORT=3030
  JWT_SECRET=pTj+DBXRkyetocsvSTE1n4r95Gq8rCnCeKvl2qDNBDc=
  # Optional: the only account that may upload images, created on startup
  ADMIN_USERNAME=admin
  ADMIN_PASSWORD=adminpassword
  # Optional, registration has no captcha without them
  TURNSTILE_SITE_KEY=1x00000000000000000000AA
  TURNSTILE_SECRET=1x0000000000000000000000000000000AA
  # Optional, how long accounts of regular users live (ISO-8601 duration, 0 keeps them forever)
  DEMO_USER_TTL=PT1H
```
These values are for local development only, the `JWT_SECRET` above is public.
Accounts of regular users are deleted after an hour, set `DEMO_USER_TTL=0` to keep them.

On a small server the memory use can be limited with these optional variables (defaults in parentheses):
```
JAVA_TOOL_OPTIONS="-Xmx256m -XX:+UseCompactObjectHeaders"    # JVM options of the backend (none, the heap may grow to 1/4 of the RAM)
DB_POOL_SIZE=3                # database connections of the backend (10)
TOMCAT_MAX_THREADS=20         # request threads of the backend (200)
DB_BUFFER_POOL_SIZE=48M       # innodb buffer pool of mariadb (128M)
```

The browser only talks to the frontend container: its nginx serves the app and proxies all `/api/` requests
to the backend inside the compose network. Frontend and api therefore share one origin, so no CORS setup is needed
and the app works under any host name the frontend is reached by.

| Target          | Description                                   |
|-----------------|-----------------------------------------------|
| `make up`       | Build and start the stack in the background   |
| `make down`     | Stop the stack, keeps the database volume     |
| `make restart`  | Rebuild and restart the stack                 |
| `make logs`     | Follow the logs of the stack                  |
| `make ps`       | Show the containers of the stack              |
| `make reset-db` | Stop the stack and delete the database volume |

## Manual without docker

### Database
Create a mariadb database with a user and password and pass those as environment in the following section.
Alternatively docker-compose can be used just for the database by creating a .env file with all `DB_*` variables
from the previous section and running `make db`.

### Backend

#### Environment
The following environment has to exist before running.
All `DB_*` variables should correspond to a running mariadb database and `JWT_SECRET` should be a valid secret key.
```
  DB_HOST=localhost
  DB_PORT=3306
  DB_DBNAME=springdb
  DB_USERNAME=maria
  DB_PASSWORD=mariapass
  BACKEND_PORT=8080
  JWT_SECRET=pTj+DBXRkyetocsvSTE1n4r95Gq8rCnCeKvl2qDNBDc=
  # Optional: the only account that may upload images, created on startup
  ADMIN_USERNAME=admin
  ADMIN_PASSWORD=adminpassword
  # Optional, registration has no captcha without them
  TURNSTILE_SITE_KEY=1x00000000000000000000AA
  TURNSTILE_SECRET=1x0000000000000000000000000000000AA
  # Optional, how long accounts of regular users live (ISO-8601 duration, 0 keeps them forever)
  DEMO_USER_TTL=PT1H
```

#### Running
Run the following after configuring the environment, it also loads the variables from the .env file in root
```
make backend-run
```
`make backend-build` builds the jar into `backend/target` without running the tests.

### Frontend

#### Environment
The frontend calls the api under the relative path `/api/`. The vite dev server proxies it to the backend,
by default at `localhost:8080`. Set the following only if the backend runs elsewhere
```
BACKEND_HOST=localhost
BACKEND_PORT=8080
```
When serving a build (`make frontend-build`) with another web server, it has to proxy `/api/` to the backend
like `frontend/nginx.conf.template` does.

#### Running
```
make install
make frontend-run
```

# Testing
All test suites also run in GitHub Actions with the same make targets, see `.github/workflows/`.
`make test` runs the backend and frontend tests, `make test-all` additionally the end to end tests.

## Backend
Unit tests (`*Test.java`) don't need a database
```
make backend-unit-test
```
Integration tests (`*IT.java`) need a mariadb and are run together with the unit tests in docker
```
make backend-test
```

## Frontend
Linting, typechecking and unit and component tests with vitest (requires node >= 22).
They can also be run on their own with `make frontend-lint`, `make frontend-typecheck` and `make frontend-unit-test`
```
make frontend-test
```

## End to end
Playwright journeys against the whole application. The stack runs at `http://localhost:3040/`, so it doesn't
collide with the regular one, and the tests wait until the backend is up.
The target starts the stack, runs the tests and stops it again
```
make e2e-test
```

## Cleanup
| Target              | Description                                   |
|---------------------|-----------------------------------------------|
| `make clean`        | Remove build output and test reports          |
| `make docker-clean` | Remove leftover test and e2e containers       |
| `make clean-all`    | Run clean and docker-clean                    |


# Credit
All fish data was taken from https://fishbase.org
