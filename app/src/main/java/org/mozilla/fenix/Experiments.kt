/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.Context
import mozilla.components.service.fretboard.ExperimentDescriptor

const val EXPERIMENTS_JSON_FILENAME = "experiments.json"
const val EXPERIMENTS_BASE_URL = "https://settings.prod.mozaws.net/v1"
const val EXPERIMENTS_BUCKET_NAME = "main"
// TODO Change this after fenix-experiments is created
const val EXPERIMENTS_COLLECTION_NAME = "focus-experiments"

object Experiments {
    val AATestDescriptor = ExperimentDescriptor("AAtest")
}

val Context.app: FenixApplication
    get() = applicationContext as FenixApplication

fun Context.isInExperiment(descriptor: ExperimentDescriptor): Boolean =
    app.fretboard.isInExperiment(this, descriptor)
