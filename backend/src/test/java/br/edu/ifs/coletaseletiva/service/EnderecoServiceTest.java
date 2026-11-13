package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.EnderecoDTO;
import br.edu.ifs.coletaseletiva.entity.Endereco;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.EnderecoForm;
import br.edu.ifs.coletaseletiva.mapper.EnderecoMapper;
import br.edu.ifs.coletaseletiva.repository.EnderecoRepository;
import br.edu.ifs.coletaseletiva.repository.MoradorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnderecoServiceTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);

    @Mock
    private EnderecoRepository repository;
    @Mock
    private MoradorRepository moradorRepository;
    @Mock
    private MoradorService moradorService;

    private EnderecoService service;
    private Morador maria;

    @BeforeEach
    void preparar() {
        service = new EnderecoService(repository, moradorRepository, moradorService, new EnderecoMapper());
        maria = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com").build();
    }

    private static EnderecoForm form(String numero, String complemento) {
        return new EnderecoForm("Rua das Flores", numero, "Centro", "Lagarto", "SE", "49400000", complemento);
    }

    @Test
    void rf02_criarNormalizaSemNumeroEComplementoVazioEVinculaAoMorador() {
        when(moradorRepository.findById(1)).thenReturn(Optional.of(maria));
        when(repository.save(any(Endereco.class))).thenAnswer(chamada -> {
            Endereco e = chamada.getArgument(0);
            e.setId(10);
            return e;
        });

        EnderecoDTO dto = service.criar(ADMINISTRADOR, 1, form("s/n", "   "));

        assertEquals(10, dto.id());
        assertEquals(1, dto.moradorId());
        assertEquals("S/N", dto.numero());
        assertNull(dto.complemento());
        verify(moradorService).autorizarAcesso(ADMINISTRADOR, 1);
    }

    @Test
    void rf02_moradorSemPermissaoNaoCriaEnderecoNemConsultaOBanco() {
        Ator outro = new Ator(Perfil.MORADOR, 2);
        doThrow(new AcessoNegadoException("erro.morador.acesso-proprio-cadastro"))
                .when(moradorService).autorizarAcesso(outro, 1);

        assertThrows(AcessoNegadoException.class, () -> service.criar(outro, 1, form("120", null)));

        verifyNoInteractions(moradorRepository, repository);
    }

    @Test
    void rf02_moradorInexistenteLancaNaoEncontradoSemGravar() {
        when(moradorRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> service.criar(ADMINISTRADOR, 99, form("120", null)));

        verify(repository, never()).save(any());
    }

    @Test
    void rf02_listarDevolveSomenteOsEnderecosDoMorador() {
        when(moradorRepository.findById(1)).thenReturn(Optional.of(maria));
        Endereco um = Endereco.builder().id(10).morador(maria).logradouro("A").numero("1").bairro("B")
                .municipio("C").uf("SE").cep("49400000").build();
        Endereco dois = Endereco.builder().id(11).morador(maria).logradouro("D").numero("S/N").bairro("E")
                .municipio("F").uf("SE").cep("49400001").build();
        when(repository.findByMoradorIdOrderById(1)).thenReturn(List.of(um, dois));

        List<EnderecoDTO> enderecos = service.listar(ADMINISTRADOR, 1);

        assertEquals(2, enderecos.size());
        assertEquals(10, enderecos.get(0).id());
        assertEquals(1, enderecos.get(1).moradorId());
    }

    @Test
    void rf02_atualizarEnderecoDeOutroMoradorEhNaoEncontradoSemGravar() {
        when(moradorRepository.findById(1)).thenReturn(Optional.of(maria));
        when(repository.findByIdAndMoradorId(50, 1)).thenReturn(Optional.empty());

        NaoEncontradoException erro = assertThrows(NaoEncontradoException.class,
                () -> service.atualizar(ADMINISTRADOR, 1, 50, form("120", null)));

        assertEquals("erro.endereco.nao-encontrado", erro.getChave());
        verify(repository, never()).save(any());
    }

    @Test
    void rf02_atualizarAlteraOsCamposMantendoODono() {
        when(moradorRepository.findById(1)).thenReturn(Optional.of(maria));
        Endereco existente = Endereco.builder().id(10).morador(maria).logradouro("Antiga").numero("1")
                .bairro("B").municipio("C").uf("SE").cep("49400000").complemento("Fundos").build();
        when(repository.findByIdAndMoradorId(10, 1)).thenReturn(Optional.of(existente));
        when(repository.save(any(Endereco.class))).thenAnswer(chamada -> chamada.getArgument(0));

        EnderecoDTO dto = service.atualizar(ADMINISTRADOR, 1, 10, form("S/N", ""));

        assertEquals("Rua das Flores", dto.logradouro());
        assertEquals("S/N", dto.numero());
        assertNull(dto.complemento());
        assertEquals(1, dto.moradorId());
    }
}
