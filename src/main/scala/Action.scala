package org.example.action

sealed trait Action
case object Sleep extends Action
case object Exercise extends Action
