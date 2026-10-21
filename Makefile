MVN ?= mvn
COMPOSE ?= docker compose
COMPOSE_TEST = $(COMPOSE) -f docker-compose.test.yml
COMPOSE_E2E = $(COMPOSE) -f docker-compose.e2e.yml
PLAYWRIGHT_INSTALL_FLAGS ?=
LOAD_ENV = if [ -f .env ]; then set -a; . ./.env; set +a; fi

.DEFAULT_GOAL := help

.PHONY: help
help: ## Show this help
	@awk 'BEGIN {FS = ":.*## "} \
		/^##@/ {printf "\n%s\n", substr($$0, 5)} \
		/^[a-zA-Z_-]+:.*## / {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

##@ Setup

.PHONY: install
install: ## Install the node dependencies of frontend and e2e
	cd frontend && npm ci
	cd e2e && npm ci

##@ Local development

.PHONY: db
db: ## Start only the database container, for running the backend locally
	$(COMPOSE) up -d db

.PHONY: test-db
test-db: ## Start only the test database container
	$(COMPOSE_TEST) up -d test-db

.PHONY: backend-run
backend-run: ## Run the backend with the variables from .env
	$(LOAD_ENV); cd backend && $(MVN) spring-boot:run

.PHONY: frontend-run
frontend-run: ## Run the vite dev server
	$(LOAD_ENV); cd frontend && npm run dev

.PHONY: backend-build
backend-build: ## Build the backend jar without tests
	cd backend && $(MVN) -B package -DskipTests

.PHONY: frontend-build
frontend-build: ## Build the frontend into frontend/dist
	cd frontend && npm run build

.PHONY: build
build: backend-build frontend-build ## Build backend and frontend

##@ Tests

.PHONY: test
test: backend-test frontend-test ## Run backend and frontend tests

.PHONY: test-all
test-all: test e2e-test ## Run all tests including e2e

.PHONY: backend-unit-test
backend-unit-test: ## Run the backend unit tests without a database
	cd backend && $(MVN) -B test

.PHONY: backend-test
backend-test: ## Run backend unit and integration tests in docker
	$(COMPOSE_TEST) build --quiet
	$(COMPOSE_TEST) up --attach test-backend --abort-on-container-exit --exit-code-from test-backend; \
		status=$$?; \
		rm -rf backend/target/surefire-reports backend/target/failsafe-reports; \
		mkdir -p backend/target; \
		$(COMPOSE_TEST) cp test-backend:/app/target/surefire-reports backend/target/; \
		$(COMPOSE_TEST) cp test-backend:/app/target/failsafe-reports backend/target/; \
		$(COMPOSE_TEST) down -v; exit $$status

.PHONY: frontend-lint
frontend-lint: ## Lint the frontend
	cd frontend && npm run lint

.PHONY: frontend-typecheck
frontend-typecheck: ## Typecheck the frontend
	cd frontend && npx tsc -b

.PHONY: frontend-unit-test
frontend-unit-test: ## Run the frontend unit tests
	cd frontend && npm test -- --run

.PHONY: frontend-test
frontend-test: frontend-lint frontend-typecheck frontend-unit-test ## Lint, typecheck and test the frontend

.PHONY: e2e-test
e2e-test: ## Run the e2e tests against a fresh e2e stack
	cd e2e && npm ci && npx playwright install $(PLAYWRIGHT_INSTALL_FLAGS) chromium && npm run typecheck
	$(COMPOSE_E2E) up -d --build
	(cd e2e && npm test); \
		status=$$?; \
		if [ $$status -ne 0 ]; then $(COMPOSE_E2E) logs; fi; \
		$(COMPOSE_E2E) down -v; exit $$status

##@ Docker

.PHONY: up
up: ## Build and start the stack in the background
	$(COMPOSE) up -d --build

.PHONY: down
down: ## Stop the stack, keeps the database volume
	$(COMPOSE) down

.PHONY: restart
restart: down up ## Rebuild and restart the stack

.PHONY: logs
logs: ## Follow the logs of the stack
	$(COMPOSE) logs -f

.PHONY: ps
ps: ## Show the containers of the stack
	$(COMPOSE) ps

##@ Cleanup

.PHONY: clean
clean: ## Remove build output and test reports
	rm -rf backend/target frontend/dist frontend/reports e2e/playwright-report e2e/test-results e2e/reports

.PHONY: docker-clean
docker-clean: ## Remove leftover test and e2e containers
	$(COMPOSE_TEST) down -v --remove-orphans
	$(COMPOSE_E2E) down -v --remove-orphans

.PHONY: clean-all
clean-all: clean docker-clean ## Run clean and docker-clean

.PHONY: reset-db
reset-db: ## Stop the stack and DELETE the database volume
	@printf 'This deletes all data in the database, continue? [y/N] '; \
		read answer; [ "$$answer" = y ] || { echo Aborted; exit 1; }
	$(COMPOSE) down -v
