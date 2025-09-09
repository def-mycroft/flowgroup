package com.mfme.kernel.adapters

import com.mfme.kernel.data.Envelope

interface ShareAdapter { suspend fun capture(): Envelope }
interface CameraAdapter { suspend fun capture(): Envelope }
interface MicAdapter { suspend fun capture(): Envelope }
interface FilesAdapter { suspend fun capture(): Envelope }
interface LocationAdapter { suspend fun capture(): Envelope }
interface SensorsAdapter { suspend fun capture(): Envelope }
