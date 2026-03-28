FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci --ignore-scripts

COPY . .
RUN npx ng build --configuration production --output-path dist/bearmq

FROM nginx:alpine AS runtime
COPY --from=build /app/dist/bearmq/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
