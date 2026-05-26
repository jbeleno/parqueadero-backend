package com.usco.parqueaderos_api.convenio.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.convenio.dto.ConvenioDTO;
import com.usco.parqueaderos_api.convenio.entity.Convenio;
import com.usco.parqueaderos_api.convenio.repository.ConvenioRepository;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ConvenioServiceTest {

    private ConvenioRepository repo;
    private ParqueaderoRepository parqRepo;
    private CurrentUserService user;
    private ConvenioService service;

    @BeforeEach
    void setup() {
        repo = Mockito.mock(ConvenioRepository.class);
        parqRepo = Mockito.mock(ParqueaderoRepository.class);
        user = Mockito.mock(CurrentUserService.class);
        service = new ConvenioService(repo, parqRepo, user);
    }

    private ConvenioDTO dtoValido() {
        ConvenioDTO d = new ConvenioDTO();
        d.setParqueaderoId(1L);
        d.setNombreComercio("Tienda X");
        d.setTipoDescuento("MONTO_FIJO");
        d.setValorDescuento(3000.0);
        return d;
    }

    private void mockParqueadero() {
        Parqueadero p = new Parqueadero();
        p.setId(1L);
        Empresa e = new Empresa(); e.setId(10L);
        p.setEmpresa(e);
        when(parqRepo.findById(1L)).thenReturn(Optional.of(p));
    }

    @Test
    @DisplayName("USER no puede crear convenios")
    void user_no_crea() {
        when(user.isAdmin()).thenReturn(false);
        when(user.isSuperAdmin()).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.save(dtoValido()));
    }

    @Test
    @DisplayName("Falta parqueaderoId -> error")
    void falta_parqueadero() {
        when(user.isAdmin()).thenReturn(true);
        ConvenioDTO d = dtoValido();
        d.setParqueaderoId(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(d));
        assertEquals("ERR_MISSING_FIELDS", ex.getErrorCode());
    }

    @Test
    @DisplayName("tipoDescuento invalido -> error")
    void tipo_invalido() {
        when(user.isAdmin()).thenReturn(true);
        ConvenioDTO d = dtoValido();
        d.setTipoDescuento("REGALO_NAVIDAD");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(d));
        assertEquals("ERR_INVALID_TIPO_DESCUENTO", ex.getErrorCode());
    }

    @Test
    @DisplayName("MONTO_FIJO sin valorDescuento -> error")
    void monto_fijo_sin_valor() {
        when(user.isAdmin()).thenReturn(true);
        ConvenioDTO d = dtoValido();
        d.setValorDescuento(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(d));
        assertEquals("ERR_INVALID_VALUE", ex.getErrorCode());
    }

    @Test
    @DisplayName("PORCENTAJE > 100 -> error")
    void porcentaje_invalido() {
        when(user.isAdmin()).thenReturn(true);
        ConvenioDTO d = new ConvenioDTO();
        d.setParqueaderoId(1L);
        d.setNombreComercio("X");
        d.setTipoDescuento("PORCENTAJE");
        d.setPorcentajeDescuento(150.0);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(d));
        assertEquals("ERR_INVALID_VALUE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Convenio valido se persiste activo=true y tipo en uppercase")
    void crear_ok() {
        when(user.isAdmin()).thenReturn(true);
        when(user.isSuperAdmin()).thenReturn(true);
        mockParqueadero();
        ConvenioDTO d = dtoValido();
        d.setTipoDescuento("monto_fijo"); // probar normalizacion
        when(repo.save(any())).thenAnswer(inv -> {
            Convenio c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });
        ConvenioDTO out = service.save(d);
        assertEquals(99L, out.getId());
        assertEquals("MONTO_FIJO", out.getTipoDescuento());
        assertTrue(out.getActivo());
    }

    @Test
    @DisplayName("ADMIN distinta empresa no puede crear convenio en parqueadero ajeno")
    void admin_otra_empresa_bloqueado() {
        when(user.isAdmin()).thenReturn(true);
        when(user.isSuperAdmin()).thenReturn(false);
        mockParqueadero();
        Mockito.doThrow(new AccessDeniedException("nope")).when(user).requireEmpresa(10L);
        assertThrows(AccessDeniedException.class, () -> service.save(dtoValido()));
    }
}
