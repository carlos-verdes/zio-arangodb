/*
 * Copyright 2022 Carlos Verdes
 *
 * SPDX-License-Identifier: MIT
 */

package io.funkode.arangodb.model

case class TransactionList(transactions: List[TransactionList.Transaction])

object TransactionList:

  case class Transaction(id: TransactionId, state: String)
