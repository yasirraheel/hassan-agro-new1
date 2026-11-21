<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{ $businessName }} Verification Code</title>
</head>
<body style="margin:0; padding:0; background-color:#F3F5F8; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color:#1E293B;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#F3F5F8; padding:32px 16px;">
        <tr>
            <td align="center">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:560px; background-color:#FFFFFF; border-radius:14px; overflow:hidden; border:1px solid #E2E8F0;">
                    <tr>
                        <td style="background-color:{{ $primaryColor }}; padding:26px 30px;">
                            <div style="font-size:12px; letter-spacing:1.2px; text-transform:uppercase; color:rgba(255,255,255,0.82); font-weight:600; margin-bottom:6px;">
                                Business Registration Verification
                            </div>
                            <div style="font-size:22px; font-weight:700; color:#FFFFFF; line-height:1.3;">
                                {{ $businessName }}
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:30px;">
                            <p style="margin:0 0 14px 0; font-size:15px; line-height:1.6; color:#1E293B;">
                                Hello {{ $userName }},
                            </p>
                            <p style="margin:0 0 22px 0; font-size:14.5px; line-height:1.65; color:#475569;">
                                Thank you for setting up <strong>{{ $businessName }}</strong>. Please enter the 6-digit verification code below in the app to confirm your email address and activate your workspace.
                            </p>

                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="margin:0 0 22px 0;">
                                <tr>
                                    <td align="center" style="background-color:{{ $softColor }}; border:1px solid #CBD5E1; border-radius:10px; padding:20px 16px;">
                                        <div style="font-size:11px; text-transform:uppercase; letter-spacing:1.4px; color:#475569; font-weight:700; margin-bottom:8px;">
                                            Verification Code
                                        </div>
                                        <div style="font-size:34px; font-weight:800; letter-spacing:8px; color:{{ $primaryColor }}; font-family:'Courier New', Courier, monospace;">
                                            {{ $otpCode }}
                                        </div>
                                    </td>
                                </tr>
                            </table>

                            <p style="margin:0 0 14px 0; font-size:13.5px; line-height:1.6; color:#475569;">
                                This code is valid for <strong>{{ $expiryMinutes }} minutes</strong> and can only be used once for <strong>{{ $email }}</strong>.
                            </p>
                            <p style="margin:0; font-size:13px; line-height:1.6; color:#64748B;">
                                If you did not request to register <strong>{{ $businessName }}</strong>, you can safely ignore this email and no account will be created.
                            </p>
                        </td>
                    </tr>
                    <tr>
                        <td style="background-color:#F8FAFC; border-top:1px solid #E2E8F0; padding:18px 30px; font-size:12px; color:#64748B; text-align:center;">
                            Sent on behalf of <strong>{{ $businessName }}</strong> &middot; {{ $year }}
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>
