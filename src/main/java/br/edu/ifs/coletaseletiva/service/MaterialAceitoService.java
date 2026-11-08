package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MaterialAceitoDTO;
import br.edu.ifs.coletaseletiva.entity.Material;
import br.edu.ifs.coletaseletiva.entity.MaterialAceito;
import br.edu.ifs.coletaseletiva.entity.MaterialAceitoId;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.VinculoForm;
import br.edu.ifs.coletaseletiva.mapper.MaterialAceitoMapper;
import br.edu.ifs.coletaseletiva.repository.MaterialAceitoRepository;
import br.edu.ifs.coletaseletiva.repository.MaterialRepository;
import br.edu.ifs.coletaseletiva.repository.PontoColetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaterialAceitoService {

    private final MaterialAceitoRepository repository;
    private final PontoColetaRepository pontoRepository;
    private final MaterialRepository materialRepository;
    private final MaterialAceitoMapper mapper;

    /**
     * Cria o vínculo se não existir; se existir, atualiza observações e limite. A chave composta
     * (ponto, material) garante que o mesmo vínculo nunca se repete. A RN05 não se aplica aqui.
     */
    @Transactional
    public MaterialAceitoDTO vincular(Ator ator, Integer pontoId, Integer materialId, VinculoForm formulario) {
        exigirAdministrador(ator);
        PontoColeta ponto = pontoRepository.findById(pontoId)
                .orElseThrow(() -> new NaoEncontradoException("erro.ponto.nao-encontrado"));
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new NaoEncontradoException("erro.material.nao-encontrado"));

        MaterialAceitoId id = new MaterialAceitoId(pontoId, materialId);
        MaterialAceito vinculo = repository.findById(id).orElseGet(() -> novo(id, ponto, material));
        vinculo.setObservacoes(nuloSeVazio(formulario.observacoes()));
        vinculo.setLimite(formulario.limite());

        return mapper.paraDTO(repository.save(vinculo));
    }

    /** Remove o vínculo. O bloqueio por solicitações abertas (RN05) entra no ciclo das solicitações. */
    @Transactional
    public void remover(Ator ator, Integer pontoId, Integer materialId) {
        exigirAdministrador(ator);
        MaterialAceito vinculo = repository.findById(new MaterialAceitoId(pontoId, materialId))
                .orElseThrow(() -> new NaoEncontradoException("erro.vinculo.nao-encontrado"));
        repository.delete(vinculo);
    }

    // ---------- apoio ----------

    private MaterialAceito novo(MaterialAceitoId id, PontoColeta ponto, Material material) {
        return MaterialAceito.builder().id(id).ponto(ponto).material(material).build();
    }

    private String nuloSeVazio(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto;
    }

    private void exigirAdministrador(Ator ator) {
        if (ator.perfil() != Perfil.ADMINISTRADOR) {
            throw new AcessoNegadoException("erro.vinculo.somente-administrador");
        }
    }
}
