/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package zio.json

import scala.compiletime.uninitialized

import zio.json.internal.{RecordingReader, RetractReader, Write}

//TODO: Remove this when this issue https://github.com/zio/zio-json/issues/487 will be fixed

class JsonRewindDecoder[A](jsonDecoder: JsonDecoder[A]) extends JsonDecoder[A]:
  private var recordingReader: RecordingReader = uninitialized
  lazy val reader = recordingReader
  override def unsafeDecode(trace: List[JsonError], in: RetractReader): A =
    recordingReader = RecordingReader(in)
    jsonDecoder.unsafeDecode(trace, recordingReader)

