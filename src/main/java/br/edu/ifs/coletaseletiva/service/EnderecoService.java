package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.EnderecoDTO;
import br.edu.ifs.coletaseletiva.entity.Endereco;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.EnderecoForm;
import br.edu.ifs.coletaseletiva.mapper.EnderecoMapper;
import br.edu.ifs.coletaseletiva.repository.EnderecoRepository;
import br.edu.ifs.coletaseletiva.repository.MoradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private static final String SEM_NUMERO = "S/N";

    private final EnderecoRepository repository;
    private final MoradorRepository moradorRepository;
    private final MoradorService moradorService;
    private final EnderecoMapper mapper;

    @Transactional
    public EnderecoDTO criar(Ator ator, Integer moradorId, EnderecoForm formulario) {
        Morador morador = moradorAutorizado(ator, moradorId);
        Endereco salvo = repository.save(mapper.paraEntidade(normalizar(formulario), morador));
        return mapper.paraDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<EnderecoDTO> listar(Ator ator, Integer moradorId) {
        moradorAutorizado(ator, moradorId);
        return repository.findByMoradorIdOrderById(moradorId).stream()
                .map(mapper::paraDTO)
                .toList();
    }

    @Transactional
    public EnderecoDTO atualizar(Ator ator, Integer moradorId, Integer id, EnderecoForm formulario) {
        moradorAutorizado(ator, moradorId);
        // Busca pelo par (id, moradorId): endereço de outro morador é tratado como inexistente (404).
        Endereco endereco = repository.findByIdAndMoradorId(id, moradorId)
                .orElseThrow(() -> new NaoEncontradoException("erro.endereco.nao-encontrado"));
        mapper.atualizar(endereco, normalizar(formulario));
        return mapper.paraDTO(repository.save(endereco));
    }

    // ---------- apoio ----------

    /** Reaproveita a regra de permissão do RF01 e garante que o morador da URL existe. */
    private Morador moradorAutorizado(Ator ator, Integer moradorId) {
        moradorService.autorizarAcesso(ator, moradorId);
        return moradorRepository.findById(moradorId)
                .orElseThrow(() -> new NaoEncontradoException("erro.morador.nao-encontrado"));
    }

    /** Os espaços já foram removidos pelo form; aqui ficam "s/n" -> "S/N" e complemento vazio -> nulo. */
    private EnderecoForm normalizar(EnderecoForm f) {
        String numero = SEM_NUMERO.equalsIgnoreCase(f.numero()) ? SEM_NUMERO : f.numero();
        String complemento = (f.complemento() == null || f.complemento().isBlank()) ? null : f.complemento();
        return new EnderecoForm(f.logradouro(), numero, f.bairro(), f.municipio(), f.uf(), f.cep(), complemento);
    }
}
