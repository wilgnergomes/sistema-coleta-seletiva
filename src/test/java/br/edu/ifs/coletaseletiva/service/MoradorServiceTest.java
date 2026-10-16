package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MoradorDTO;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.form.MoradorAtualizacaoForm;
import br.edu.ifs.coletaseletiva.form.MoradorCriacaoForm;
import br.edu.ifs.coletaseletiva.mapper.MoradorMapper;
import br.edu.ifs.coletaseletiva.repository.MoradorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoradorServiceTest {

    @Mock
    private MoradorRepository repository;

    private MoradorService service;

    @BeforeEach
    void preparar() {
        service = new MoradorService(repository, new MoradorMapper());
    }

    @Test
    void rf01_criarNormalizaOsDadosEGrava() {
        when(repository.existsByEmail("maria@exemplo.com")).thenReturn(false);
        when(repository.save(any(Morador.class))).thenAnswer(chamada -> {
            Morador m = chamada.getArgument(0);
            m.setId(1);
            return m;
        });

        MoradorDTO dto = service.criar(new MoradorCriacaoForm("  Maria ", " Maria@Exemplo.COM ", "   "));

        assertEquals(1, dto.id());
        assertEquals("Maria", dto.nome());
        assertEquals("maria@exemplo.com", dto.email());
        assertNull(dto.fone());
        assertEquals(SituacaoCadastro.ATIVO, dto.situacao());
    }

    @Test
    void rf01_emailRepetidoEhRejeitadoSemGravar() {
        when(repository.existsByEmail("maria@exemplo.com")).thenReturn(true);

        assertThrows(ConflitoRegraException.class,
                () -> service.criar(new MoradorCriacaoForm("Maria", "maria@exemplo.com", null)));

        verify(repository, never()).save(any());
    }

    @Test
    void rf01_moradorNaoPodeAlterarPropriaSituacao() {
        Morador existente = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));

        Ator ator = new Ator(Perfil.MORADOR, 1);
        MoradorAtualizacaoForm form =
                new MoradorAtualizacaoForm("Maria", "maria@exemplo.com", null, SituacaoCadastro.INATIVO);

        assertThrows(AcessoNegadoException.class, () -> service.atualizar(ator, 1, form));
        verify(repository, never()).save(any());
    }

    @Test
    void rf01_administradorInativaMorador() {
        Morador existente = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailAndIdNot("maria@exemplo.com", 1)).thenReturn(false);
        when(repository.save(any(Morador.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Ator ator = new Ator(Perfil.ADMINISTRADOR, null);
        MoradorDTO dto = service.atualizar(ator, 1,
                new MoradorAtualizacaoForm("Maria", "maria@exemplo.com", null, SituacaoCadastro.INATIVO));

        assertEquals(SituacaoCadastro.INATIVO, dto.situacao());
    }

    @Test
    void rf01_operadorNaoAcessaCadastroDeMorador() {
        Ator ator = new Ator(Perfil.OPERADOR, 7);

        assertThrows(AcessoNegadoException.class, () -> service.buscar(ator, 1));
    }
}