# Java Spring Boot MongoDB Sample MFlix Application

This is a full-stack movie browsing application built with Java Spring Boot and Next.js, demonstrating MongoDB operations using the `sample_mflix` dataset. The application showcases CRUD operations, aggregations, and MongoDB Search using Spring Data MongoDB.

## Project Structure

```
├── README.md
├── client/                 # Next.js frontend (TypeScript)
└── server/                 # Java Spring Boot backend
    ├── src/
    ├── pom.xml
    ├── .env.example
    └── mvnw
```

## Data Limitations

The `sample_mflix` dataset contains movies released up to **2016**. Searching for movies from 2017 or later will return no results. This is a limitation of the sample dataset, not the application.

## Prerequisites

- **Java 21** or higher
- **Node.js 20** or higher
- **MongoDB Atlas cluster or local deployment** with the `sample_mflix` dataset loaded
  - [Load sample data](https://www.mongodb.com/docs/atlas/sample-data/)
- **Maven** (included via Maven Wrapper)
- **Voyage AI API key** (For MongoDB Vector Search)
  - [Get a Voyage AI API key](https://www.voyageai.com/)

## Verify Requirements

Before getting started, run the verification script to check if you have the required runtime:

```bash
./check-requirements-java.sh --pre
```

This checks that Java and JAVA_HOME are configured correctly. Run with `--help` for more options.

## Getting Started

### 1. Configure the Backend

Navigate to the Java Spring server directory:

```bash
cd server
```

Create a `.env` file from the example:

```bash
cp .env.example .env
```

Edit the `.env` file and set your MongoDB connection string:

```env
# MongoDB Connection
MONGODB_URI=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/sample_mflix?retryWrites=true&w=majority

# Voyage AI Configuration (optional - required for Vector Search)
VOYAGE_API_KEY=your_voyage_api_key

# Server Configuration
PORT=3001

# CORS Configuration
CORS_ORIGINS=http://localhost:3000
```

**Note:** Replace `<username>`, `<password>`, and `<cluster>` with your
actual MongoDB Atlas credentials. Replace `your_voyage_api_key` with
your key.

### 2. Start the Backend Server

From the `server` directory, run:

```bash
# Using Maven Wrapper (recommended)
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run
```

The server will start on `http://localhost:3001`. You can verify it's running by visiting:
- API root: http://localhost:3001/
- API documentation (Swagger UI): http://localhost:3001/swagger-ui.html

### 3. Configure and Start the Frontend

Open a new terminal and navigate to the client directory:

```bash
cd client
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

The Next.js application will start on `http://localhost:3000`.

### 4. Access the Application

Open your browser and navigate to:
- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:3001
- **API Documentation:** http://localhost:3001/swagger-ui.html

## Features

- **Browse Movies:** View a paginated list of movies from the
  sample_mflix dataset
- **CRUD Operations:** Create, read, update and delete movies by using
  the MongoDB Java driver
- **Search:** Search movies with filters by using MongoDB Search
- **Vector Search:** Search movie plots with similar search terms by
  using MongoDB Vector Search
- **Aggregations:** View data aggregations and analytics built with
  aggregation pipelines

## Development

### Backend Development

The Java Spring Boot backend uses:
- **Spring Data MongoDB** for database operations
- **Spring Boot Web** for REST API
- **SpringDoc OpenAPI** for API documentation
- **Maven** for dependency management

To run tests:

```bash
cd server
./mvnw test
```

### Frontend Development

The Next.js frontend uses:
- **React 19** with TypeScript
- **Next.js 16** with App Router
- **Turbopack** for fast development builds

#### Development Mode

For active development with hot reloading and fast refresh:

```bash
cd client
npm run dev
```

This starts the development server on `http://localhost:3000` with Turbopack for fast rebuilds.

#### Production Build

To create an optimized production build and run it:

```bash
cd client
npm run build  # Creates optimized production build
npm start      # Starts production server
```

The production build:
- Minifies and optimizes JavaScript and CSS
- Optimizes images and assets
- Generates static pages where possible
- Provides better performance for end users

#### Linting

To check code quality:

```bash
cd client
npm run lint
```

## Verify Setup

After completing the setup, run the full verification to ensure everything is configured correctly:

```bash
./check-requirements-java.sh
```

This checks your Java environment, Maven dependencies, `.env` configuration, and frontend setup.

## Issues

If you have problems running the sample app, please check the following:

- [ ] Verify that you have set your MongoDB connection string in the `.env` file.
- [ ] Verify that you have started the Java Spring server.
- [ ] Verify that you have started the Next.js client.
- [ ] Verify that you have no firewalls blocking access to the server or client ports.

If you have verified the above and still have issues, please
[open an issue](https://github.com/mongodb/docs-sample-apps/issues/new/choose)
on the source repository `mongodb/docs-sample-apps`.
