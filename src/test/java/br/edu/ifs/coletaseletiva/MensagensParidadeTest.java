package br.edu.ifs.coletaseletiva;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Garante que os dois idiomas têm exatamente as mesmas chaves (ADR 0023): uma mensagem
 * esquecida em um dos arquivos apareceria para o cliente como a própria chave.
 */
class MensagensParidadeTest {

    @Test
    void i18n_osDoisArquivosTemAsMesmasChaves() throws IOException {
        Properties portugues = carregar("messages.properties");
        Properties ingles = carregar("messages_en.properties");

        Set<String> soNoPortugues = new TreeSet<>(portugues.stringPropertyNames());
        soNoPortugues.removeAll(ingles.stringPropertyNames());
        Set<String> soNoIngles = new TreeSet<>(ingles.stringPropertyNames());
        soNoIngles.removeAll(portugues.stringPropertyNames());

        assertEquals(Set.of(), soNoPortugues, "chaves que faltam em messages_en.properties");
        assertEquals(Set.of(), soNoIngles, "chaves que faltam em messages.properties");
    }

    @Test
    void i18n_nenhumaMensagemEstaVazia() throws IOException {
        for (String arquivo : new String[]{"messages.properties", "messages_en.properties"}) {
            Properties mensagens = carregar(arquivo);
            for (String chave : mensagens.stringPropertyNames()) {
                assertFalse(mensagens.getProperty(chave).isBlank(), arquivo + ": mensagem vazia em " + chave);
            }
        }
    }

    private Properties carregar(String arquivo) throws IOException {
        try (InputStream entrada = getClass().getClassLoader().getResourceAsStream(arquivo)) {
            Properties propriedades = new Properties();
            propriedades.load(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            return propriedades;
        }
    }
}
