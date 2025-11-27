# Development Dockerfile for React frontend with Vite HMR
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install all dependencies (including dev dependencies)
RUN npm install

# Copy source files
COPY . .

# Expose Vite dev server port
EXPOSE 3000

# Run Vite dev server with hot reload
CMD ["npm", "run", "dev"]
