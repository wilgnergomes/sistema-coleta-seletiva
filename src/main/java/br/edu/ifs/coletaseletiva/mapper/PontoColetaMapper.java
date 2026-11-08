package br.edu.ifs.coletaseletiva.mapper;

import br.edu.ifs.coletaseletiva.dto.EnderecoPontoDTO;
import br.edu.ifs.coletaseletiva.dto.MaterialAceitoDTO;
import br.edu.ifs.coletaseletiva.dto.PontoColetaDTO;
import br.edu.ifs.coletaseletiva.entity.EnderecoPonto;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.form.EnderecoForm;
import br.edu.ifs.coletaseletiva.form.PontoColetaForm;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PontoColetaMapper {

    public PontoColeta paraEntidade(PontoColetaForm form) {
        PontoColeta ponto = new PontoColeta();
        ponto.setSituacao(SituacaoCadastro.ATIVO);
        atualizar(ponto, form);
        return ponto;
    }

    /** Copia os campos do form; a situação só muda se vier informada e o recolhimento ausente vale "não". */
    public void atualizar(PontoColeta ponto, PontoColetaForm form) {
        ponto.setNome(form.nome());
        ponto.setContato(form.contato());
        ponto.setHorarios(form.horarios());
        ponto.setRecolhimentoDomiciliar(Boolean.TRUE.equals(form.recolhimentoDomiciliar()));
        if (form.situacao() != null) {
            ponto.setSituacao(form.situacao());
        }
        ponto.setEndereco(paraEndereco(form.endereco()));
    }

    /** Os materiais aceitos (RF05) chegam já convertidos; o mapper só os coloca na resposta. */
    public PontoColetaDTO paraDTO(PontoColeta ponto, List<MaterialAceitoDTO> materiaisAceitos) {
        EnderecoPonto e = ponto.getEndereco();
        return new PontoColetaDTO(
                ponto.getId(),
                ponto.getNome(),
                ponto.getContato(),
                ponto.getHorarios(),
                ponto.isRecolhimentoDomiciliar(),
                ponto.getSituacao(),
                new EnderecoPontoDTO(e.getLogradouro(), e.getNumero(), e.getBairro(), e.getMunicipio(),
                        e.getUf(), e.getCep(), e.getComplemento()),
                materiaisAceitos
        );
    }

    private EnderecoPonto paraEndereco(EnderecoForm f) {
        return EnderecoPonto.builder()
                .logradouro(f.logradouro())
                .numero(f.numero())
                .bairro(f.bairro())
                .municipio(f.municipio())
                .uf(f.uf())
                .cep(f.cep())
                .complemento(f.complemento())
                .build();
    }
}
