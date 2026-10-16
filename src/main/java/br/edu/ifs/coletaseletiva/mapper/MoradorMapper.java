package br.edu.ifs.coletaseletiva.mapper;

import br.edu.ifs.coletaseletiva.dto.MoradorDTO;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.form.MoradorAtualizacaoForm;
import br.edu.ifs.coletaseletiva.form.MoradorCriacaoForm;
import org.springframework.stereotype.Component;

@Component
public class MoradorMapper {

    public Morador paraEntidade(MoradorCriacaoForm form) {
        return Morador.builder()
                .nome(form.nome())
                .email(form.email())
                .fone(form.fone())
                .build();
    }

    public void atualizar(Morador morador, MoradorAtualizacaoForm form) {
        morador.setNome(form.nome());
        morador.setEmail(form.email());
        morador.setFone(form.fone());
        if (form.situacao() != null) {
            morador.setSituacao(form.situacao());
        }
    }

    public MoradorDTO paraDTO(Morador morador) {
        return new MoradorDTO(
                morador.getId(),
                morador.getNome(),
                morador.getEmail(),
                morador.getFone(),
                morador.getSituacao()
        );
    }
}