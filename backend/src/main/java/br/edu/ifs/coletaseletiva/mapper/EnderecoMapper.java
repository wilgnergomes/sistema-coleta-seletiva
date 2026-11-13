package br.edu.ifs.coletaseletiva.mapper;

import br.edu.ifs.coletaseletiva.dto.EnderecoDTO;
import br.edu.ifs.coletaseletiva.entity.Endereco;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.form.EnderecoForm;
import org.springframework.stereotype.Component;

@Component
public class EnderecoMapper {

    public Endereco paraEntidade(EnderecoForm form, Morador morador) {
        Endereco endereco = new Endereco();
        endereco.setMorador(morador);
        atualizar(endereco, form);
        return endereco;
    }

    /** Copia os campos do form. O dono (morador) nunca muda. */
    public void atualizar(Endereco endereco, EnderecoForm form) {
        endereco.setLogradouro(form.logradouro());
        endereco.setNumero(form.numero());
        endereco.setBairro(form.bairro());
        endereco.setMunicipio(form.municipio());
        endereco.setUf(form.uf());
        endereco.setCep(form.cep());
        endereco.setComplemento(form.complemento());
    }

    public EnderecoDTO paraDTO(Endereco endereco) {
        return new EnderecoDTO(
                endereco.getId(),
                endereco.getMorador().getId(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getBairro(),
                endereco.getMunicipio(),
                endereco.getUf(),
                endereco.getCep(),
                endereco.getComplemento()
        );
    }
}
