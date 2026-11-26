package fr.soft_labs.eventbyte.adapters.out.stock;

import fr.soft_labs.eventbyte.domain.booking.BookingLine;
import fr.soft_labs.eventbyte.ports.out.TicketStock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.ArrayList;
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

    /** Une reprise du job ne doit pas restituer deux fois les mêmes places. */
    private static final RedisScript<Long> GIVE_BACK_ONCE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 1 then
              return 0
            end
            for i = 2, #KEYS do
              if redis.call('EXISTS', KEYS[i]) == 1 then
                redis.call('INCRBY', KEYS[i], ARGV[i - 1])
              end
            end
            redis.call('SET', KEYS[1], '1')
            return 1
            """, Long.class);

    private static final String AVAILABLE_FROM_DATABASE = """
            select t.quantity - coalesce((
                select sum(l.quantity)
                from booking_lines l join bookings b on b.id = l.booking_id
                where l.ticket_id = t.id
                  -- Pas REQUESTED : la réservation est enregistrée en REQUESTED avant de prendre le stock,
                  -- elle se retirerait ses propres places (et celles des demandes concurrentes non servies).
                  and b.status in ('RESERVED', 'AWAITING_PAYMENT', 'CONFIRMED')
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
        lines.forEach(line -> initializeIfAbsent(line.ticketId()));
        Long taken = redis.execute(TAKE, keys(lines), quantities(lines));
        return Long.valueOf(1).equals(taken);
    }

    /** Lecture seule côté client, mais initialise la clé si besoin (setIfAbsent : sans risque face aux réservations). */
    @Override
    public int available(String ticketId) {
        initializeIfAbsent(ticketId);
        return Integer.parseInt(redis.opsForValue().get(key(ticketId)));
    }

    @Override
    public void giveBack(List<BookingLine> lines) {
        redis.execute(GIVE_BACK, keys(lines), quantities(lines));
    }

    @Override
    public void giveBackOnce(String bookingId, List<BookingLine> lines) {
        List<String> scriptKeys = new ArrayList<>();
        scriptKeys.add("stock:returned:booking:" + bookingId);
        scriptKeys.addAll(keys(lines));
        redis.execute(GIVE_BACK_ONCE, scriptKeys, quantities(lines));
    }

    @Override
    public void refresh(Collection<String> ticketIds) {
        if (!ticketIds.isEmpty()) {
            redis.delete(ticketIds.stream().map(RedisTicketStock::key).toList());
        }
    }

    private void initializeIfAbsent(String ticketId) {
        String key = key(ticketId);
        if (Boolean.TRUE.equals(redis.hasKey(key))) {
            return;
        }
        List<Long> available = jdbc.queryForList(AVAILABLE_FROM_DATABASE, Long.class, ticketId);
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
