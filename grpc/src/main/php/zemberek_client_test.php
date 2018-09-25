<?php
require dirname(__FILE__).'/vendor/autoload.php';

@include_once dirname(__FILE__).'/Zemberek/Langid/LanguageIdServiceClient.php';
@include_once dirname(__FILE__).'/language_id.pb.php';

function lang_id($name)
{
    $client = new Zemberek\Langid\LanguageIdServiceClient('localhost:6789', [
        'credentials' => Grpc\ChannelCredentials::createInsecure(),
    ]);
    $request = new Zemberek\Langid\DetectRequest();
    $request->setInput($name);
    list($reply, $status) = $client->Detect($request)->wait();
    $message = $reply->getLangId();

    return $message;
}

$input = !empty($argv[1]) ? $argv[1] : 'Merhaba Dünya!'."\n";
echo $input;
echo "Dil = ".lang_id($input);
echo "\n";
