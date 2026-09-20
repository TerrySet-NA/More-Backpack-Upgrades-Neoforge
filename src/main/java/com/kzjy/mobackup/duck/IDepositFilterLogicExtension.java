/*
 * Copyright (C) 2026 TerrySet
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package com.kzjy.mobackup.duck;

import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import javax.annotation.Nullable;

public interface IDepositFilterLogicExtension {
    void mobackup$setRsStorage(@Nullable StorageNetworkComponent storage);
    @Nullable StorageNetworkComponent mobackup$getRsStorage();
}