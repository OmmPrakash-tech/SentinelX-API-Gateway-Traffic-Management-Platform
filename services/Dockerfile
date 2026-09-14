FROM node:24-alpine
WORKDIR /app
ENV HOST=0.0.0.0
COPY services/server.mjs server.mjs
USER node
EXPOSE 9100
CMD ["node","server.mjs"]
