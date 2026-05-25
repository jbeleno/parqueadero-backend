// AuthService.java — rotación del refresh token (Figura 13)
@Transactional
public AuthResponse refresh(RefreshRequest request) {
    RefreshToken rt = refreshTokenService.verificarRefreshToken(
            request.getRefreshToken());

    // Revoca el refresh anterior y emite uno nuevo (rotación)
    refreshTokenService.revocarToken(rt);
    RefreshToken nuevoRt = refreshTokenService.crearRefreshToken(rt.getUsuario());

    UserDetails userDetails = userDetailsService.loadUserByUsername(
            rt.getUsuario().getCorreo());
    Long empresaId = rt.getUsuario().getEmpresa() != null
            ? rt.getUsuario().getEmpresa().getId() : null;
    String newAccessToken = jwtService.generateToken(userDetails, empresaId);

    return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(nuevoRt.getToken())
            .correo(rt.getUsuario().getCorreo())
            .tipo("Bearer")
            .expiresIn(jwtService.getAccessExpirationMs() / 1000)
            .empresaId(empresaId)
            .build();
}
