package fr.soft_labs.eventbyte.adapters.out.stock;

import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.ports.out.TicketStock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Stock disponible par ticket dans Redis : une clé "stock:ticket:{id}" = nombre de places restantes.
 * <p>
 * La prise de stock est un script Lua : Redis l'exécute en un seul bloc, sans qu'aucune autre commande
 * ne s'intercale. Deux demandes simultanées du dernier ticket ne peuvent donc pas réussir toutes les deux,
 * et une réservation de plusieurs tickets prend tout ou rien.
 * <p>
 * Initialisation à la demande : si la clé n'existe pas, stock total Strapi moins les places déjà tenues
 * en base, posé avec SETNX (le premier gagne).
 * <p>
 * EB-30 : quand Strapi modifie un événement, {@link #refresh} supprime les clés ; elles sont recalculées
 * à la réservation suivante avec la nouvelle quantité. Stock baissé sous le réservé : disponible 0,
 * aucune réservation existante n'est annulée.
 */
@Component
class RedisTicketStock implements TicketStock {

    static final String KEY_PREFIX = "stock:ticket:";

    /** Vérifie toutes les lignes, puis décrémente toutes les lignes ; 1 = pris, 0 = stock insuffisant. */
    private static final RedisScript<Long> TAKE = new DefaultRedisScript<>("""
            for i, key in ipairs(KEYS) do
              local available = tonumber(redis.call('GET', key))
              if available == nil or available < tonumber(ARGV[i]) then
                return 0
              end
            end
            for i, key in ipairs(KEYS) do
              redis.call('DECRBY', key, ARGV[i])
            end
            return 1
            """, Long.class);

    /** Rend les places, seulement sur les clés existantes (une clé absente sera recalculée depuis la base). */
    private static final RedisScript<Long> GIVE_BACK = new DefaultRedisScript<>("""
            for i, key in ipairs(KEYS) do
              if redis.call('EXISTS', key) == 1 then
                redis.call('INCRBY', key, ARGV[i])
              end
            end
            return 1
            """, Long.class);

    private static final String AVAILABLE_FROM_DATABASE = """
            select t.quantity - coalesce((
                select sum(l.quantity)
                from booking_lines l join bookings b on b.id = l.booking_id
                where l.ticket_id = t.id
                  -- REQUESTED compte aussi : une réservation en cours a pu prendre le stock Redis
                  -- sans être encore enregistrée en RESERVED. Au pire on sous-estime un instant, jamais de survente.
                  and b.status in ('REQUESTED', 'RESERVED', 'AWAITING_PAYMENT', 'CONFIRMED')
            ), 0)
            from tickets t
            where t.id = ?
            """;

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;

    RedisTicketStock(StringRedisTemplate redis, JdbcTemplate jdbc) {
        this.redis = redis;
        this.jdbc = jdbc;
    }

    @Override
    public boolean tryTake(List<BookingLine> lines) {
        lines.forEach(this::initializeIfAbsent);
        Long taken = redis.execute(TAKE, keys(lines), quantities(lines));
        return Long.valueOf(1).equals(taken);
    }

    @Override
    public void giveBack(List<BookingLine> lines) {
        redis.execute(GIVE_BACK, keys(lines), quantities(lines));
    }

    @Override
    public void refresh(Collection<String> ticketIds) {
        if (!ticketIds.isEmpty()) {
            redis.delete(ticketIds.stream().map(RedisTicketStock::key).toList());
        }
    }

    private void initializeIfAbsent(BookingLine line) {
        String key = key(line.ticketId());
        if (Boolean.TRUE.equals(redis.hasKey(key))) {
            return;
        }
        List<Long> available = jdbc.queryForList(AVAILABLE_FROM_DATABASE, Long.class, line.ticketId());
        long value = available.isEmpty() || available.get(0) == null ? 0 : Math.max(0, available.get(0));
        redis.opsForValue().setIfAbsent(key, Long.toString(value));
    }

    static String key(String ticketId) {
        return KEY_PREFIX + ticketId;
    }

    private static List<String> keys(List<BookingLine> lines) {
        return lines.stream().map(l -> key(l.ticketId())).toList();
    }

    private static Object[] quantities(List<BookingLine> lines) {
        return lines.stream().map(l -> Integer.toString(l.quantity())).toArray();
    }
}
