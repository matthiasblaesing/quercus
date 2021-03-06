package com.caucho.quercus.env.xdebug;

import java.util.Map;

public class ContextNamesCommand extends XdebugCommand
{

  @Override
  protected XdebugResponse getInternalResponse(String commandName,
      Map<String, String> parameters, String transactionId,
      XdebugConnection conn) {
    return new XdebugResponse(
        null,
        "<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"context_names\" transaction_id=\""
            + transactionId
            + "\"><context name=\"Locals\" id=\""
            + CONTEXT_ID_LOCALS
            + "\"></context><context name=\"Globals\" id=\""
            + CONTEXT_ID_GLOBALS + "\"></context></response>", transactionId);
  }
}
