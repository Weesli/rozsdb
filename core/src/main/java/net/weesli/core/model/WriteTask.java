package net.weesli.core.model;

import net.weesli.api.database.Database;
import net.weesli.api.model.ObjectId;

public record WriteTask(Database database, ObjectId objectId, byte[] data) {
}
