# Emit only a bounded progress summary. Never return tool arguments/results or raw errors.
def known_tool:
  . as $t | ["phone_status","phone_ui","phone_screenshot","phone_launch","phone_tap",
    "phone_swipe","phone_key","phone_input","phone_click"] | index($t)!=null;
def safe_text:
  gsub("sk-[A-Za-z0-9_-]{8,}";"[redacted]")
  | gsub("Bearer +[A-Za-z0-9._~+/-]{8,}";"Bearer [redacted]")
  | gsub("data:image[^[:space:]]*";"[image omitted]")
  | gsub("[A-Za-z0-9+/]{200,}={0,2}";"[binary omitted]")
  | .[0:600];
reduce (inputs | fromjson?) as $e
  ({tool_calls_completed:0,tool_calls_failed:0,last_tool:null,messages:[],tools:{},
     business_outcome_verified:false,automatic_resubmission:false};
    ($e.item // {}) as $i
    | if $i.type=="mcp_tool_call" and ($i.tool|known_tool) then
        .last_tool={name:$i.tool,status:($i.status // "unknown")}
        | if $e.type=="item.completed" then
            (if $i.status=="failed" or $i.result.isError==true then "failed" else "completed" end) as $s
            | if $s=="failed" then .tool_calls_failed+=1 else .tool_calls_completed+=1 end
            | .tools[$i.tool][$s]=((.tools[$i.tool][$s]//0)+1)
            | .last_tool.status=$s
          else . end
      elif $e.type=="item.completed" and $i.type=="agent_message" and ($i.text|type)=="string" then
        .messages=((.messages + [($i.text|safe_text)])[-2:])
      else . end)
| . + {source:"codex_cli_events",instruction:"Tool completion records accepted actions, not proof that the full user task succeeded. Query the same task id; never automatically resubmit."}
