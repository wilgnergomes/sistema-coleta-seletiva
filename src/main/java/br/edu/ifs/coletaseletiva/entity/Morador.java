package br.edu.ifs.coletaseletiva.entity;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mor_morador")
public class Morador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mor_nr_id")
    private Integer id;

    @Column(name = "mor_tx_nome", nullable = false, length = 80)
    private String nome;

    @Column(name = "mor_tx_email", nullable = false, length = 150)
    private String email;

    @Column(name = "mor_tx_fone", length = 20)
    private String fone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "mor_tx_situacao", nullable = false, length = 10)
    private SituacaoCadastro situacao = SituacaoCadastro.ATIVO;

}
