package fr.soft_labs.eventbyte.ports.in;

/** Expire les réservations impayées arrivées à échéance et restitue leurs places. */
public interface ExpireUnpaidBookings {

    int expireOverdueBookings();
}

