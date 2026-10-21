# Only return known, normalized errors. Never copy raw provider text or tool data.
reduce (inputs | fromjson? | select(.type == "turn.failed" or .type == "error")) as $e
  (""; ($e.error.message // $e.message // .) | if type == "string" then . else "" end)
| . as $message
| (if $code == 124 or $code == 137 then
     {code:"task_timeout",message:"The task reached the local phone bridge deadline. Inspect progress for actions already attempted; an empty final answer does not mean no action happened."}
   elif $message | test("idle timeout waiting for SSE";"i") then
     {code:"model_idle_timeout",message:"The model API did not send stream data within the configured idle timeout."}
   elif $message | test("stream closed before response.completed";"i") then
     {code:"model_stream_closed",message:"The model API stream closed before response.completed."}
   elif $message | test("unexpected status 50[0-9]";"i") then
     {code:"provider_unavailable",message:"The model API gateway returned a 5xx server error."}
   elif $message | test("unexpected status (401|403)";"i") then
     {code:"provider_auth_rejected",message:"The model API rejected authentication or access."}
   elif $message | test("unexpected status 429";"i") then
     {code:"provider_rate_limited",message:"The model API returned HTTP 429; check the provider rate limit or quota."}
   else
     {code:"codex_failed",message:"Codex exited without a complete result. Inspect the saved task logs; do not automatically resubmit."}
   end)
| . + {automatic_resubmission:false}
