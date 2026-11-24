"""Relaciones, estereotipos y notas declarados a mano (el resto sale del código)."""

AGREGADOS = ['Publicacion', 'Contratacion', 'Perfil', 'Wallet', 'Notificacion']
EVENTOS = ['SolicitudRecibida', 'ContratacionAceptada', 'PagoConfirmado', 'ReprogramacionPropuesta',
           'ReprogramacionAceptada', 'ContratacionCancelada', 'SesionCompletada']


def dominio(rel):
    """Dominio: composición, asociaciones y dependencias con multiplicidades reales. Entre módulos, solo ids."""
    # publicacion
    rel('Publicacion', 'PublicacionId', 'assoc', 'id', '0..1', '1')
    rel('Publicacion', 'TipoPublicacion', 'assoc', 'tipo', '0..*', '1')
    rel('Publicacion', 'Modalidad', 'assoc', 'modalidad', '0..*', '1')
    rel('Publicacion', 'EstadoPublicacion', 'assoc', 'estado', '0..*', '1')
    rel('Publicacion', 'FranjaDisponibilidad', 'comp', 'franjas', '1', '0..*')
    rel('Publicacion', 'Promocion', 'comp', 'promociones', '1', '0..*')
    rel('Publicacion', 'CambioEstado', 'comp', 'historial', '1', '0..*')
    rel('FranjaDisponibilidad', 'FranjaId', 'assoc', 'id', '0..1', '1')
    rel('Publicacion', 'PrecioBase', 'dep', est='create')
    rel('Publicacion', 'PrecioConPromocion', 'dep', est='create')
    rel('PrecioConPromocion', 'Precio', 'agg', 'envuelto', '0..*', '1')
    rel('PrecioConPromocion', 'Promocion', 'assoc', 'promocion', '0..*', '1')
    rel('PublicacionRepository', 'Publicacion', 'dep', est='use')
    # contratacion
    rel('Contratacion', 'ContratacionId', 'assoc', 'id', '0..1', '1')
    rel('Contratacion', 'Solicitud', 'comp', 'solicitud', '1', '1')
    rel('Contratacion', 'EstadoContratacion', 'assoc', 'estado', '0..*', '1')
    rel('Contratacion', 'PropuestaReprogramacion', 'comp', 'propuesta', '1', '0..1')
    rel('Contratacion', 'Reintegro', 'comp', 'reintegro', '1', '0..1')
    rel('Contratacion', 'Calificacion', 'comp', 'calificaciones', '1', '0..2')
    rel('Contratacion', 'CambioEstado', 'comp', 'historial', '1', '0..*')
    rel('Contratacion', 'EventoDominio', 'assoc', 'eventos', '0..1', '0..*')
    rel('Calificacion', 'CalificacionId', 'assoc', 'id', '0..1', '1')
    rel('Calificacion', 'Puntaje', 'assoc', 'puntaje', '0..*', '1')
    rel('Contratacion', 'PoliticaReintegro', 'dep', est='use')
    rel('PoliticaReintegroFactory', 'PoliticaReintegro', 'dep', est='create')
    rel('PoliticaReintegroFactory', 'ReintegroPorAnticipacion', 'assoc', 'porAnticipacion', '0..1', '1')
    rel('PoliticaReintegroFactory', 'ReintegroTotal', 'assoc', 'total', '0..1', '1')
    rel('PoliticaReintegro', 'Reintegro', 'dep', est='create')
    rel('ContratacionRepository', 'Contratacion', 'dep', est='use')
    # perfil
    rel('Perfil', 'TipoPerfil', 'assoc', 'tipo', '0..*', '1')
    rel('Perfil', 'CanalNotificacion', 'assoc', 'canal', '0..*', '1')
    rel('Perfil', 'Reputacion', 'comp', 'reputacion', '1', '1')
    rel('Perfil', 'DisponibilidadSemanal', 'comp', 'disponibilidad', '1', '0..*')
    rel('Perfil', 'CambioEstado', 'comp', 'historial', '1', '0..*')
    rel('Reputacion', 'Puntaje', 'dep', est='use')
    rel('PerfilRepository', 'Perfil', 'dep', est='use')
    # wallet
    rel('Wallet', 'Movimiento', 'comp', 'movimientosNuevos', '1', '0..*')
    rel('Movimiento', 'MovimientoId', 'assoc', 'id', '0..1', '1')
    rel('Movimiento', 'TipoMovimiento', 'assoc', 'tipo', '0..*', '1')
    rel('RetencionDePagos', 'Wallet', 'dep', est='use')
    rel('WalletRepository', 'Wallet', 'dep', est='use')
    # notificacion
    rel('Notificacion', 'NotificacionId', 'assoc', 'id', '0..1', '1')
    rel('Notificacion', 'EventoDominio', 'dep', est='use')
    rel('NotificacionRepository', 'Notificacion', 'dep', est='use')
    # shared
    rel('Pagina', 'PaginaSolicitada', 'dep', est='use')


def extra(rel):
    """Relaciones de las otras capas que no salen de los campos."""
    rel('EstrategiaRecomendacion', 'Publicacion', 'dep', est='use')


def estereotipos(T):
    e = {n: 'Aggregate Root' for n in AGREGADOS}
    e.update({
        'FranjaDisponibilidad': 'Entity',
        'EstadoPublicacion': 'State', 'EstadoContratacion': 'State',
        'Precio': 'Decorator', 'PrecioBase': 'Decorator', 'PrecioConPromocion': 'Decorator',
        'PoliticaReintegro': 'Strategy', 'ReintegroPorAnticipacion': 'Strategy', 'ReintegroTotal': 'Strategy',
        'PoliticaReintegroFactory': 'Simple Factory',
        'RetencionDePagos': 'Domain Service',
        'EventoDominio': 'Domain Event',
        'EstrategiaRecomendacion': 'Strategy', 'AfinidadPorHabilidades': 'Strategy',
        'PopularidadReciente': 'Strategy',
        'PublicadorEventos': 'Observer', 'SuscriptorEvento': 'Observer', 'NotificadorDeInteresados': 'Observer',
        'CanalEnvioFactory': 'Simple Factory',
        'EmailAdapter': 'Adapter', 'PushAdapter': 'Adapter', 'ServicioIdentidadSimulado': 'Adapter',
    })
    for k, t in T.items():
        if k in e:
            continue
        if t['capa'] == 'dominio':
            if '/state' in t['pkg'] and t['kind'] == 'record':
                e[k] = 'State'
            elif t['pkg'].endswith('evento'):
                e[k] = 'Domain Event'
            elif t['name'].endswith('Repository'):
                e[k] = 'Repository'
            elif t['kind'] == 'record':
                e[k] = 'Value Object'
        elif t['pkg'].endswith('/dto'):
            e[k] = 'DTO'
        elif t['name'].endswith('Entity'):
            e[k] = 'JPA Entity'
        elif t['name'].endswith('Service'):
            e[k] = 'Service'
    return e


def notas():
    return {
        'EstadoPublicacion': 'STATE\nCada estado decide qué transiciones admite\n(publicar, pausar, reanudar, cerrar, cancelar).\n'
                             'Sin él: un switch por estado en cada método.',
        'EstadoContratacion': 'STATE\nCada estado decide qué acciones admite\n(aceptar, pagar, iniciar, completar, cancelar).\n'
                              'Sin él: ifs de estado repartidos en Contratacion.',
        'PrecioConPromocion': 'DECORATOR\nCada promoción envuelve al precio anterior;\nse acumulan en el orden en que se agregaron.\n'
                              'Sin él: un if por combinación de descuentos.',
        'PoliticaReintegro': 'STRATEGY\nReintegro 100/50/0 % según anticipación (48 h / 24 h),\no total si cancela el proveedor.\n'
                             'Otra política es otra clase: Contratacion no cambia.',
        'PoliticaReintegroFactory': 'SIMPLE FACTORY\nElige la política según quién cancela.\n'
                                    'Quien cancela no conoce las clases concretas.',
        'EstrategiaRecomendacion': 'STRATEGY\nAfinidad por habilidades o popularidad reciente.\n'
                                   'Cambiar el criterio no toca BusquedaService.',
        'PublicadorEventos': 'OBSERVER\nLos Services publican los eventos del dominio;\n'
                             'los suscriptores (NotificadorDeInteresados) reaccionan.\n'
                             'Sumar un suscriptor no cambia a quien publica.',
        'CanalEnvioFactory': 'SIMPLE FACTORY\nElige el EnvioNotificacion según el canal del perfil\n(interna, email o push).',
        'EmailAdapter': 'ADAPTER\nAdapta el proveedor de email simulado a EnvioNotificacion\n'
                        '(lo mismo PushAdapter y ServicioIdentidadSimulado).\nCambiar de proveedor no toca la aplicación.',
        'UiControllers': 'Contrato de la cátedra entre la UI y el backend:\nno se modifica. Lo que falta va en las\n'
                         'interfaces *Operaciones del equipo.',
    }
