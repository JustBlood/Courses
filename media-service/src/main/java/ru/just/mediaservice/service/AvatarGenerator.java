package ru.just.mediaservice.service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;

public class AvatarGenerator {

    public static ByteArrayOutputStream generateAvatar(int avatarSize, String username) {
        if (avatarSize % 12 != 0) {
            throw new IllegalArgumentException("Avatar size must be a multiple of 12");
        }

        // Фоновый цвет
        Color backgroundColor = Color.decode("#f2f1f2");

        // Генерация псевдослучайных байт из хэша MD5
        byte[] hashBytes = getMd5Bytes(username);

        // Получаем основной цвет из первых 3 байт хэша
        Color mainColor = new Color(
                (hashBytes[0] & 0xFF) / 2 + 128,
                (hashBytes[1] & 0xFF) / 2 + 128,
                (hashBytes[2] & 0xFF) / 2 + 128
        );

        // Генерация матрицы заполнения (6x12)
        boolean[][] needColor = new boolean[12][12];
        for (int row = 0; row < 12; row++) {
            for (int col = 0; col < 6; col++) { // Левая половина
                int byteIndex = 3 + (row * 6 + col) / 8;
                int bitIndex = (row * 6 + col) % 8;
                boolean isColored = (hashBytes[byteIndex] >> (7 - bitIndex) & 1) == 1;
                needColor[row][col] = isColored;
                needColor[row][11 - col] = isColored; // Отражаем на правую половину
            }
        }

        // Создание изображения
        BufferedImage image = new BufferedImage(avatarSize, avatarSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Заливка фона
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, avatarSize, avatarSize);

        // Размер блока
        int blockSize = avatarSize / 12;

        // Рисование цветных блоков
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                if (needColor[i][j]) {
                    g2d.setColor(mainColor);
                    g2d.fillRect(j * blockSize, i * blockSize, blockSize, blockSize);
                }
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos;
    }

    private static byte[] getMd5Bytes(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(input.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Error generating MD5 hash", e);
        }
    }
}

