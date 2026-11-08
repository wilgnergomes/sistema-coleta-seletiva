package br.edu.ifs.coletaseletiva.mapper;

import br.edu.ifs.coletaseletiva.dto.OperadorDTO;
import br.edu.ifs.coletaseletiva.entity.Operador;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.form.OperadorForm;
import org.springframework.stereotype.Component;

@Component
public class OperadorMapper {

    public Operador paraEntidade(OperadorForm form, PontoColeta ponto) {
        Operador operador = new Operador();
        operador.setSituacao(SituacaoCadastro.ATIVO);
        atualizar(operador, form, ponto);
        return operador;
    }

    /** Copia os campos do form; a situação só muda se vier informada. O ponto já chega resolvido. */
    public void atualizar(Operador operador, OperadorForm form, PontoColeta ponto) {
        operador.setNome(form.nome());
        operador.setEmail(form.email());
        operador.setPonto(ponto);
        if (form.situacao() != null) {
            operador.setSituacao(form.situacao());
        }
    }

    public OperadorDTO paraDTO(Operador operador) {
        return new OperadorDTO(
                operador.getId(),
                operador.getNome(),
                operador.getEmail(),
                operador.getPonto().getId(),
                operador.getSituacao()
        );
    }
}
