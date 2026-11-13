package br.edu.ifs.coletaseletiva.mapper;

import br.edu.ifs.coletaseletiva.dto.MoradorDTO;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.form.MoradorForm;
import org.springframework.stereotype.Component;

@Component
public class MoradorMapper {

    /** Não copia {@code situacao}: todo morador nasce ATIVO (padrão da entidade), seja o que o corpo trouxer. */
    public Morador paraEntidade(MoradorForm form) {
        Morador morador = new Morador();
        morador.setNome(form.nome());
        morador.setEmail(form.email());
        morador.setFone(form.fone());
        return morador;
    }

    public void atualizar(Morador morador, MoradorForm form) {
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