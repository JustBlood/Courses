#!/bin/bash

# Ожидание пока сервер MinIO поднимется
sleep 5

# Инициализация MinIO Client (mc)
mc alias set myminio http://localhost:9000 admin adminadmin

# Применяем политику к бакету
mc anonymous set public myminio/users/*/avatar/*
