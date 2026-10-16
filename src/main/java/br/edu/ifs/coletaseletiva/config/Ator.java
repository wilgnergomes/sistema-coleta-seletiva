package br.edu.ifs.coletaseletiva.config;

import br.edu.ifs.coletaseletiva.enumeration.Perfil;

import java.util.Objects;

public record Ator(Perfil perfil, Integer usuarioId) {
    public Ator {
        Objects.requireNonNull(perfil, "perfil é obrigatório");
    }
}